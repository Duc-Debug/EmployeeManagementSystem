package com.hrm.employeemanagement.application.dto.allocation;

import java.time.LocalDateTime;

public record AllocationNotificationItemResult(
        Long id,
        Long recipientId,
        String recipientName,
        Long senderId,
        String senderName,
        String type,
        String targetType,
        Long targetId,
        String title,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {
}
