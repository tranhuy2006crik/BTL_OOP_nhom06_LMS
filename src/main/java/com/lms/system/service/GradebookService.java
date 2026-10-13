package com.lms.system.service;

import com.lms.system.dto.GradeForm;
import com.lms.system.dto.GradingCourseRow;
import com.lms.system.dto.StudentCourseProgress;
import com.lms.system.dto.StudentGradeRow;
import com.lms.system.dto.SubmissionForGrading;
import com.lms.system.repository.GradebookRepository;
import com.lms.system.repository.LmsWorkflowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GradebookService {

    private final GradebookRepository gradebookRepository;
    private final LmsWorkflowRepository workflowRepository;

    public GradebookService(
            GradebookRepository gradebookRepository,
            LmsWorkflowRepository workflowRepository
    ) {
        this.gradebookRepository = gradebookRepository;
        this.workflowRepository = workflowRepository;
    }

    @Transactional(readOnly = true)
    public StudentOverview studentOverview(String email) {
        // Xác định học viên từ email phiên đăng nhập, không nhận mã người dùng từ biểu mẫu.
        Long studentId = workflowRepository.requireActiveUserId(email);
        return new StudentOverview(
                gradebookRepository.findStudentGrades(studentId),
                gradebookRepository.findStudentProgress(studentId)
        );
    }

    @Transactional(readOnly = true)
    public List<SubmissionForGrading> submissionsForGrading(Long courseId, String email) {
        // Kiểm tra quyền trước khi lấy nội dung bài làm và thông tin điểm của học viên.
        requireManager(courseId, email);
        return gradebookRepository.findSubmissionsForGrading(courseId);
    }

    @Transactional(readOnly = true)
    public List<GradingCourseRow> coursesForGrading(String email) {
        Long userId = workflowRepository.requireActiveUserId(email);
        return gradebookRepository.findCoursesForGrading(userId);
    }

    @Transactional
    public void saveGrade(Long courseId, Long submissionId, GradeForm form, String email) {
        Long graderId = requireManager(courseId, email);
        // Khóa bài nộp trong transaction và lấy điểm tối đa để tránh lưu vượt thang điểm.
        var maxGrade = gradebookRepository.lockSubmissionMaxGrade(courseId, submissionId);
        if (form.getScore().compareTo(maxGrade) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Điểm không được vượt quá điểm tối đa " + maxGrade
            );
        }
        String feedback = form.getFeedback() == null || form.getFeedback().isBlank()
                ? null
                : form.getFeedback().trim();
        // Repository đặt released_at về NULL; nếu sửa điểm đã công bố, cần công bố lại để học viên thấy điểm mới.
        gradebookRepository.saveGrade(submissionId, graderId, form.getScore(), feedback);
    }

    @Transactional
    public void releaseGrade(Long courseId, Long submissionId, String email) {
        requireManager(courseId, email);
        // Xác nhận bài nộp thuộc khóa này và hợp lệ trước khi đánh dấu điểm đã công bố.
        gradebookRepository.lockSubmissionMaxGrade(courseId, submissionId);
        gradebookRepository.releaseGrade(courseId, submissionId);
    }

    public boolean canManageCourse(Long courseId, String email) {
        Long userId = workflowRepository.requireActiveUserId(email);
        return workflowRepository.canManageCourse(userId, courseId);
    }

    private Long requireManager(Long courseId, String email) {
        Long userId = workflowRepository.requireActiveUserId(email);
        // Chặn học viên hoặc người ngoài khóa truy cập chức năng quản lý điểm.
        if (!workflowRepository.canManageCourse(userId, courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chỉ giảng viên của khóa học mới được chấm điểm"
            );
        }
        return userId;
    }

    public record StudentOverview(
            List<StudentGradeRow> grades,
            List<StudentCourseProgress> courseProgress
    ) {
        public StudentOverview {
            grades = List.copyOf(grades);
            courseProgress = List.copyOf(courseProgress);
        }
    }
}
