package com.lms.system.service;

import com.lms.system.dto.AssignmentForm;
import com.lms.system.dto.CourseAssignmentView;
import com.lms.system.dto.CourseFileView;
import com.lms.system.dto.CourseResourceView;
import com.lms.system.dto.SubmissionForGrading;
import com.lms.system.repository.CourseworkRepository;
import com.lms.system.repository.CourseworkRepository.FileRecord;
import com.lms.system.repository.LmsWorkflowRepository;
import com.lms.system.service.CourseFileStorageService.StoredCourseFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CourseworkService {

    private static final int MAX_SUBMISSION_TEXT_LENGTH = 100_000;

    private final CourseworkRepository courseworkRepository;
    private final LmsWorkflowRepository workflowRepository;
    private final CourseFileStorageService fileStorage;

    public CourseworkService(
            CourseworkRepository courseworkRepository,
            LmsWorkflowRepository workflowRepository,
            CourseFileStorageService fileStorage
    ) {
        this.courseworkRepository = courseworkRepository;
        this.workflowRepository = workflowRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional(readOnly = true)
    public SectionCoursework sectionCoursework(Long courseId, Long sectionId, String email) {
        CourseAccess access = requireCourseAccess(courseId, email);
        workflowRepository.requireSectionInCourse(sectionId, courseId);
        return new SectionCoursework(
                courseworkRepository.findAssignments(courseId, sectionId, access.userId(), access.canManage()),
                courseworkRepository.findResources(courseId, sectionId, access.canManage())
        );
    }

    @Transactional
    public Long createAssignment(
            Long courseId,
            Long sectionId,
            AssignmentForm form,
            List<MultipartFile> attachments,
            String email
    ) {
        validateAssignment(form);
        Long teacherId = requireManager(courseId, sectionId, email);
        List<StoredCourseFile> stored = storeFiles(attachments);
        try {
            cleanupFilesIfTransactionRollsBack(stored);
            Long assignmentId = courseworkRepository.createAssignment(
                    courseId, sectionId, teacherId, form.getTitle(), form.getDescription(),
                    form.getDueAt(), form.getMaxGrade(), form.isAllowLateSubmission());
            attachFiles(teacherId, assignmentId, stored, "INSTRUCTIONS");
            return assignmentId;
        } catch (RuntimeException exception) {
            fileStorage.deleteAll(stored, exception);
            throw exception;
        }
    }

    @Transactional
    public void updateAssignment(
            Long courseId,
            Long sectionId,
            Long assignmentId,
            AssignmentForm form,
            List<MultipartFile> additionalAttachments,
            String email
    ) {
        validateAssignment(form);
        Long teacherId = requireManager(courseId, sectionId, email);
        List<StoredCourseFile> stored = storeFiles(additionalAttachments);
        try {
            cleanupFilesIfTransactionRollsBack(stored);
            courseworkRepository.updateAssignment(
                    courseId, sectionId, assignmentId, form.getTitle(), form.getDescription(),
                    form.getDueAt(), form.getMaxGrade(), form.isAllowLateSubmission());
            attachFiles(teacherId, assignmentId, stored, "INSTRUCTIONS");
        } catch (RuntimeException exception) {
            fileStorage.deleteAll(stored, exception);
            throw exception;
        }
    }

    @Transactional
    public Long createResource(
            Long courseId,
            Long sectionId,
            String title,
            String description,
            List<MultipartFile> uploads,
            String email
    ) {
        validateResource(title, description);
        Long teacherId = requireManager(courseId, sectionId, email);
        List<StoredCourseFile> stored = storeFiles(uploads);
        if (stored.isEmpty()) {
            throw new IllegalArgumentException("Tài liệu phải có ít nhất một tệp đính kèm");
        }
        try {
            cleanupFilesIfTransactionRollsBack(stored);
            Long resourceId = courseworkRepository.createResource(
                    courseId, sectionId, teacherId, title, description);
            attachFiles(teacherId, resourceId, stored, "RESOURCE");
            return resourceId;
        } catch (RuntimeException exception) {
            fileStorage.deleteAll(stored, exception);
            throw exception;
        }
    }

    @Transactional
    public Long submit(
            Long courseId,
            Long assignmentId,
            String textContent,
            List<MultipartFile> uploads,
            String email
    ) {
        CourseAccess access = requireCourseAccess(courseId, email);
        if (access.canManage()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Giảng viên không thể nộp bài thay học viên");
        }
        String content = normalizeSubmissionText(textContent);
        List<StoredCourseFile> stored = storeFiles(uploads);
        if ((content == null || content.isBlank()) && stored.isEmpty()) {
            throw new IllegalArgumentException("Hãy nhập nội dung hoặc đính kèm ít nhất một tệp");
        }
        try {
            cleanupFilesIfTransactionRollsBack(stored);
            List<Long> fileIds = saveFiles(access.userId(), stored).stream().map(CourseFileView::id).toList();
            return courseworkRepository.submit(
                    courseId, assignmentId, access.userId(), content, fileIds, LocalDateTime.now());
        } catch (RuntimeException exception) {
            fileStorage.deleteAll(stored, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DownloadableFile downloadActivityFile(Long courseId, Long fileId, String email) {
        CourseAccess access = requireCourseAccess(courseId, email);
        FileRecord file = courseworkRepository.findActivityFile(courseId, fileId);
        requireVisibleToStudent(file, access);
        return readFile(file);
    }

    @Transactional(readOnly = true)
    public DownloadableFile downloadSubmissionFile(
            Long courseId, Long submissionId, Long fileId, String email
    ) {
        CourseAccess access = requireCourseAccess(courseId, email);
        FileRecord file = courseworkRepository.findSubmissionFile(courseId, submissionId, fileId);
        if (!access.canManage() && !file.studentId().equals(access.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tải tệp bài nộp này");
        }
        requireVisibleToStudent(file, access);
        return readFile(file);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CourseFileView>> submissionFilesForGrading(List<SubmissionForGrading> submissions) {
        return courseworkRepository.findSubmissionFilesForIds(
                submissions.stream().map(SubmissionForGrading::submissionId).toList());
    }

    private Long requireManager(Long courseId, Long sectionId, String email) {
        CourseAccess access = requireCourseAccess(courseId, email);
        if (!access.canManage()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ giảng viên khóa học mới được quản lý hoạt động");
        }
        workflowRepository.requireSectionInCourse(sectionId, courseId);
        return access.userId();
    }

    private CourseAccess requireCourseAccess(Long courseId, String email) {
        Long userId = workflowRepository.requireActiveUserId(email);
        workflowRepository.requireCourseNotArchived(courseId);
        boolean canManage = workflowRepository.canManageCourse(userId, courseId);
        if (!canManage && !workflowRepository.isActiveStudent(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập khóa học");
        }
        return new CourseAccess(userId, canManage);
    }

    private void requireVisibleToStudent(FileRecord file, CourseAccess access) {
        if (access.canManage()) {
            return;
        }
        if (!"PUBLISHED".equals(file.courseStatus())
                || !"VISIBLE".equals(file.sectionStatus())
                || !"VISIBLE".equals(file.activityStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp");
        }
    }

    private DownloadableFile readFile(FileRecord file) {
        try {
            return new DownloadableFile(
                    file.originalName(), file.mimeType(), fileStorage.read(file.storageKey()));
        } catch (java.nio.file.NoSuchFileException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tệp không còn tồn tại", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc tệp khóa học", exception);
        }
    }

    private void attachFiles(Long uploaderId, Long activityId, List<StoredCourseFile> stored, String purpose) {
        List<Long> fileIds = saveFiles(uploaderId, stored).stream().map(CourseFileView::id).toList();
        courseworkRepository.attachFilesToActivity(activityId, fileIds, purpose);
    }

    private List<CourseFileView> saveFiles(Long uploaderId, List<StoredCourseFile> stored) {
        return stored.stream().map(file -> courseworkRepository.saveFile(uploaderId, file)).toList();
    }

    private List<StoredCourseFile> storeFiles(List<MultipartFile> uploads) {
        try {
            return fileStorage.storeAll(uploads);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu tệp tải lên", exception);
        }
    }

    private void cleanupFilesIfTransactionRollsBack(List<StoredCourseFile> stored) {
        if (stored.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Course file uploads require an active database transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    fileStorage.deleteAll(
                            stored, new IllegalStateException("Database transaction rolled back after file upload"));
                }
            }
        });
    }

    private static void validateAssignment(AssignmentForm form) {
        if (form == null || form.getTitle() == null || form.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tiêu đề bài tập không được để trống");
        }
        BigDecimal maxGrade = form.getMaxGrade();
        if (maxGrade == null || maxGrade.signum() <= 0
                || maxGrade.scale() > 2 || maxGrade.precision() - maxGrade.scale() > 5) {
            throw new IllegalArgumentException("Điểm tối đa phải lớn hơn 0 và không vượt quá 99999.99");
        }
    }

    private static void validateResource(String title, String description) {
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Tiêu đề tài liệu không hợp lệ");
        }
        if (description != null && description.length() > 10000) {
            throw new IllegalArgumentException("Mô tả tài liệu tối đa 10000 ký tự");
        }
    }

    private static String normalizeSubmissionText(String textContent) {
        if (textContent == null || textContent.isBlank()) {
            return null;
        }
        String content = textContent.trim();
        if (content.length() > MAX_SUBMISSION_TEXT_LENGTH) {
            throw new IllegalArgumentException("Nội dung bài nộp tối đa 100000 ký tự");
        }
        return content;
    }

    private record CourseAccess(Long userId, boolean canManage) {
    }

    public record SectionCoursework(
            List<CourseAssignmentView> assignments,
            List<CourseResourceView> resources
    ) {
        public SectionCoursework {
            assignments = List.copyOf(assignments);
            resources = List.copyOf(resources);
        }
    }

    public record DownloadableFile(String originalName, String mimeType, byte[] content) {
        public DownloadableFile {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
