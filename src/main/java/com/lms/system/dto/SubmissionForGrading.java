package com.lms.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmissionForGrading(
        Long submissionId,
        Long assignmentId,
        String assignmentTitle,
        String studentName,
        String studentEmail,
        String status,
        LocalDateTime submittedAt,
        Integer attemptNumber,
        String textContent,
        String attachedFileNames,
        BigDecimal maxGrade,
        BigDecimal score,
        String feedback,
        LocalDateTime gradedAt,
        LocalDateTime releasedAt
) {
}
