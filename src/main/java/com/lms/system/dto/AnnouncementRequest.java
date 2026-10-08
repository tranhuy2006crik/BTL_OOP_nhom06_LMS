package com.lms.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Dữ liệu client gửi để đăng thông báo; title và content không được để trống.
public record AnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {
}