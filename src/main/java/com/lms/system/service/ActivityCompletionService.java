package com.lms.system.service;

import com.lms.system.repository.ActivityCompletionRepository;
import com.lms.system.repository.LmsWorkflowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class ActivityCompletionService {

    private final ActivityCompletionRepository completionRepository;
    private final LmsWorkflowRepository workflowRepository;

    public ActivityCompletionService(
            ActivityCompletionRepository completionRepository,
            LmsWorkflowRepository workflowRepository
    ) {
        this.completionRepository = completionRepository;
        this.workflowRepository = workflowRepository;
    }

    @Transactional(readOnly = true)
    public Set<Long> completedActivityIds(Long courseId, Long sectionId, String email) {
        Long userId = requireStudentAccess(courseId, email);
        workflowRepository.requireSectionInCourse(sectionId, courseId);
        return completionRepository.findCompletedActivityIds(courseId, sectionId, userId);
    }

    @Transactional
    public void setCompleted(
            Long courseId,
            Long sectionId,
            Long activityId,
            boolean completed,
            String email
    ) {
        Long userId = requireStudentAccess(courseId, email);
        workflowRepository.requireSectionInCourse(sectionId, courseId);
        completionRepository.setCompleted(courseId, sectionId, activityId, userId, completed);
    }

    private Long requireStudentAccess(Long courseId, String email) {
        Long userId = workflowRepository.requireActiveUserId(email);
        workflowRepository.requireCourseNotArchived(courseId);
        if (!workflowRepository.isActiveStudent(userId, courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Chỉ học viên đang tham gia khóa học mới được cập nhật tiến độ");
        }
        return userId;
    }
}
