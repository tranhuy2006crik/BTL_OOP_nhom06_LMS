package com.lms.system.dto;

import java.time.LocalDateTime;

// Kết quả ghi danh, phản ánh membership đã được tạo hoặc kích hoạt lại.
public record EnrollmentResponse(
        Long courseId,
        Long userId,
        String status,
        LocalDateTime joinedAt
) {
}