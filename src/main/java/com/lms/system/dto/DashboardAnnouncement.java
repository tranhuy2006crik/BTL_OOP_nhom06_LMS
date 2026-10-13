package com.lms.system.dto;

import java.time.LocalDateTime;

public record DashboardAnnouncement(
        Long courseId,
        String courseTitle,
        String title,
        String content,
        LocalDateTime publishedAt
) {
}
