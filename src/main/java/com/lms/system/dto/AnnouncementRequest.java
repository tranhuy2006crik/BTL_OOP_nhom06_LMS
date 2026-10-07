package com.lms.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {
}