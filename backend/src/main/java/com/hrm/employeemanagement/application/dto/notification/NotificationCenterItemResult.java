package com.hrm.employeemanagement.application.dto.notification;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.notification.NotificationLevel;

public record NotificationCenterItemResult(
        Long id,
        Long eventId,
        String eventType,
        NotificationLevel level,
        String title,
        String message,
        String relatedEntityType,
        String relatedEntityId,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}
