package com.lms.system.dto;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long courseId,
        Long userId,
        String status,
        LocalDateTime joinedAt
) {
}