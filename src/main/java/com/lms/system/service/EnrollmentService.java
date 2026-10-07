package com.lms.system.service;

import com.lms.system.dto.EnrollmentResponse;
import com.lms.system.repository.LmsWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final LmsWorkflowRepository repository;

    public EnrollmentService(LmsWorkflowRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EnrollmentResponse enroll(Long courseId, String email) {
        Long userId = repository.requireActiveUserId(email);
        repository.requirePublishedCourse(courseId);
        return repository.enroll(courseId, userId);
    }
}