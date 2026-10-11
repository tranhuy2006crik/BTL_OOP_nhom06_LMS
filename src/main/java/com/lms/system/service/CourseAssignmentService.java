package com.lms.system.service;

import com.lms.system.model.AssignmentSubmission;
import com.lms.system.model.Course;
import com.lms.system.model.CourseAssignment;
import com.lms.system.model.CourseMaterial;
import com.lms.system.model.Permission;
import com.lms.system.model.User;
import com.lms.system.repository.AssignmentSubmissionRepository;
import com.lms.system.repository.CourseAssignmentRepository;
import com.lms.system.repository.CourseMaterialRepository;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class CourseAssignmentService {

    private static final Set<String> ALLOWED_MATERIAL_TYPES = Set.of(
            "application/pdf",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/gif"
    );

    private final CourseAssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseAssignmentService(
            CourseAssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            CourseMaterialRepository materialRepository,
            CourseRepository courseRepository,
            UserRepository userRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseAssignment> listAssignments(Long courseId) {
        ensureCourseExists(courseId);
        return assignmentRepository.findByCourseIdOrderByDueAtAsc(courseId);
    }

    @Transactional(readOnly = true)
    public CourseAssignment getAssignment(Long courseId, Long assignmentId) {
        ensureCourseExists(courseId);
        return assignmentRepository.findByIdAndCourseId(assignmentId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment was not found in this course"));
    }

    @Transactional(readOnly = true)
    public List<CourseMaterial> listMaterials(Long courseId) {
        ensureCourseExists(courseId);
        return materialRepository.findByCourseIdOrderByUploadedAtDesc(courseId);
    }

    @Transactional
    public CourseAssignment createAssignment(
            Long courseId,
            Long teacherId,
            String title,
            String description,
            String instructions,
            LocalDateTime dueAt,
            Integer maxScore,
            boolean allowLate,
            boolean published
    ) {
        ensureCourseExists(courseId);
        User teacher = userRepository.findWithAccessById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher account was not found"));
        if (!teacher.hasPermission(Permission.MANAGE_COURSE_CONTENT, courseId)) {
            throw new AccessDeniedException("You are not allowed to create assignments for this course");
        }

        CourseAssignment assignment = CourseAssignment.create(
                courseId,
                teacherId,
                title,
                description,
                instructions,
                dueAt,
                maxScore,
                allowLate,
                published
        );
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public AssignmentSubmission submitAssignment(
            Long courseId,
            Long assignmentId,
            Long studentId,
            String textAnswer,
            MultipartFile attachment
    ) {
        CourseAssignment assignment = getAssignment(courseId, assignmentId);
        User student = userRepository.findWithAccessById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student account was not found"));
        if (!student.hasPermission(Permission.SUBMIT_ASSIGNMENT, courseId)) {
            throw new AccessDeniedException("You are not allowed to submit this assignment");
        }
        if (assignment.isPastDue(LocalDateTime.now()) && !assignment.isAllowLate()) {
            throw new IllegalStateException("The deadline for this assignment has passed");
        }
        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, studentId)) {
            throw new IllegalStateException("This assignment has already been submitted by this student");
        }
        String storedFileName = null;
        String storedPath = null;
        if (attachment != null && !attachment.isEmpty()) {
            FileUpload saved = saveFile(attachment, "submissions");
            storedFileName = saved.originalFileName();
            storedPath = saved.storedPath();
        }

        AssignmentSubmission submission = AssignmentSubmission.create(
                assignmentId,
                studentId,
                textAnswer,
                storedFileName,
                storedPath,
                LocalDateTime.now()
        );
        submission = submissionRepository.save(submission);
        if (submission.getStatus() == null) {
            submission.markSubmitted(LocalDateTime.now());
        }
        return submissionRepository.save(submission);
    }

    @Transactional
    public AssignmentSubmission gradeSubmission(
            Long courseId,
            Long assignmentId,
            Long submissionId,
            Long teacherId,
            Integer grade,
            String feedback
    ) {
        CourseAssignment assignment = getAssignment(courseId, assignmentId);
        User teacher = userRepository.findWithAccessById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher account was not found"));
        if (!teacher.hasPermission(Permission.GRADE_SUBMISSION, courseId)) {
            throw new AccessDeniedException("You are not allowed to grade submissions for this course");
        }
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission was not found"));
        if (!assignmentId.equals(submission.getAssignmentId())) {
            throw new IllegalArgumentException("Submission does not belong to this assignment");
        }
        if (grade != null && grade > assignment.getMaxScore()) {
            throw new IllegalArgumentException("Grade cannot exceed the maximum score for this assignment");
        }
        submission.grade(grade, feedback);
        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public AssignmentSubmission getStudentSubmission(Long courseId, Long assignmentId, Long studentId) {
        getAssignment(courseId, assignmentId);
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("No submission found for this student"));
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmission> listSubmissions(Long courseId, Long assignmentId) {
        getAssignment(courseId, assignmentId);
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
    }

    @Transactional
    public CourseMaterial uploadMaterial(
            Long courseId,
            Long teacherId,
            String title,
            String description,
            MultipartFile file
    ) {
        ensureCourseExists(courseId);
        User teacher = userRepository.findWithAccessById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher account was not found"));
        if (!teacher.hasPermission(Permission.MANAGE_COURSE_CONTENT, courseId)) {
            throw new AccessDeniedException("You are not allowed to upload course materials");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A file is required");
        }
        FileUpload saved = saveFile(file, "materials");
        CourseMaterial material = CourseMaterial.create(
                courseId,
                teacherId,
                title,
                description,
                saved.fileName(),
                saved.originalFileName(),
                saved.contentType(),
                saved.size(),
                saved.storedPath()
        );
        return materialRepository.save(material);
    }

    @Transactional(readOnly = true)
    public Resource downloadMaterial(Long courseId, Long materialId) {
        CourseMaterial material = materialRepository.findByIdAndCourseId(materialId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Material was not found in this course"));
        Path path = Paths.get(material.getStoragePath());
        if (!Files.exists(path)) {
            throw new IllegalStateException("Stored file is missing");
        }
        return new FileSystemResource(path);
    }

    private Course ensureCourseExists(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course was not found"));
    }

    private FileUpload saveFile(MultipartFile file, String folder) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MATERIAL_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type. Please upload PDF, slide, document, image, or ZIP file.");
        }
        if (file.getSize() > 2L * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds the 2 MB maximum size.");
        }

        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String safeName = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", folder);
        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new FileUpload(safeName, originalName, contentType, file.getSize(), target.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store uploaded file", exception);
        }
    }

    public record FileUpload(String fileName, String originalFileName, String contentType, long size, String storedPath) {
    }
}
