package com.lms.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CourseSubmissionView(
        Long id,
        Integer attemptNumber,
        String status,
        LocalDateTime submittedAt,
        List<CourseFileView> files
) {
    public CourseSubmissionView {
        files = List.copyOf(files);
    }
}
