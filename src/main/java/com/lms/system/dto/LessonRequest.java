package com.lms.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotBlank String content
) {
}