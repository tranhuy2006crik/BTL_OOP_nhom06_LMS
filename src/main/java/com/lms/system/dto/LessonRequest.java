package com.lms.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Dữ liệu client gửi khi tạo/sửa bài học; nội dung bắt buộc, tiêu đề tối đa 200 ký tự.
public record LessonRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotBlank String content
) {
}