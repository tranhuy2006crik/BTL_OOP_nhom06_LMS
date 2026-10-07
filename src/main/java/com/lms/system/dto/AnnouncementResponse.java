package com.lms.system.dto;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        Long courseId,
        Long authorId,
        String title,
        String content,
        String status,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}