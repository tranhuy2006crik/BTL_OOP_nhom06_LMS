package com.lms.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CourseAssignmentView(
        Long id,
        String title,
        String description,
        Integer position,
        String status,
        LocalDateTime dueAt,
        BigDecimal maxGrade,
        boolean allowLateSubmission,
        List<CourseFileView> files,
        CourseSubmissionView latestSubmission
) {
    public CourseAssignmentView {
        files = List.copyOf(files);
    }
}
