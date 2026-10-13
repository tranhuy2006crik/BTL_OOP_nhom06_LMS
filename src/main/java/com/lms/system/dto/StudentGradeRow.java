package com.lms.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentGradeRow(
        Long courseId,
        String courseTitle,
        Long assignmentId,
        String assignmentTitle,
        BigDecimal maxGrade,
        LocalDateTime dueAt,
        Long submissionId,
        Integer attemptNumber,
        String submissionStatus,
        LocalDateTime submittedAt,
        BigDecimal score,
        String feedback,
        LocalDateTime gradedAt
) {
}
