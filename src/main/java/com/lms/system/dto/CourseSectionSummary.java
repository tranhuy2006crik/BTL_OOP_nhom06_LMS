package com.lms.system.dto;

public record CourseSectionSummary(
        Long id,
        String title,
        String description,
        int position,
        String status
) {
}
