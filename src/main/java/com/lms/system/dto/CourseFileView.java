package com.lms.system.dto;

public record CourseFileView(
        Long id,
        String originalName,
        String mimeType,
        long sizeBytes
) {
}
