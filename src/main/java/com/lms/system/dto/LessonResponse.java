package com.lms.system.dto;

import java.time.LocalDateTime;

public record LessonResponse(
        Long id,
        String title,
        String description,
        String content,
        Integer position,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}