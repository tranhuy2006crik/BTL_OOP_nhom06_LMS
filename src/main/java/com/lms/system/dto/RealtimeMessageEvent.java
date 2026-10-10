package com.lms.system.dto;

import java.time.LocalDateTime;

public record RealtimeMessageEvent(
        Long id,
        Long senderId,
        String senderName,
        String body,
        LocalDateTime sentAt
) {
}
