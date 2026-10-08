package com.lms.system.dto;

import java.time.LocalDateTime;

// Dữ liệu trả về cho bài học, gồm metadata của activity và nội dung trong bảng lessons.
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