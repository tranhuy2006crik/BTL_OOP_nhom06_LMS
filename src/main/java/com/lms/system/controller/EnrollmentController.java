package com.lms.system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.system.dto.EnrollmentResponse;
import com.lms.system.service.EnrollmentService;

@RestController
@RequestMapping("/api/courses/{courseId}/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return enrollmentService.enroll(courseId, user.getUsername());
    }   
}