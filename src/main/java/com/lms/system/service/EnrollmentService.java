package com.lms.system.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.system.dto.EnrollmentResponse;
import com.lms.system.repository.LmsWorkflowRepository;

@Service
public class EnrollmentService {

    private final LmsWorkflowRepository repository;

    public EnrollmentService(LmsWorkflowRepository repository) {
        this.repository = repository;
    }

    // Transaction bao quanh kiểm tra khóa học và cập nhật membership.
    @Transactional
    public EnrollmentResponse enroll(Long courseId, String email) {
        Long userId = repository.requireActiveUserId(email);
        // Chỉ khóa học đã công bố mới cho phép học viên tự ghi danh.
        repository.requirePublishedCourse(courseId);
        return repository.enroll(courseId, userId);
    }
}