package com.hrm.employeemanagement.application.dto.notification;

import java.time.LocalDateTime;

public record NotificationResult(
        Long id,
        Long recipientId,
        Long senderId,
        String senderName,
        String senderEmail,
        String type,
        String targetType,
        Long targetId,
        String title,
        String content,
        boolean isRead,
        LocalDateTime createdAt) {
}

