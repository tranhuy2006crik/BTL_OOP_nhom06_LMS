package com.lms.system.dto;

import java.time.LocalDateTime;

// Dữ liệu thông báo trả về; publishedAt có thể null nếu bản ghi đang là bản nháp.
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