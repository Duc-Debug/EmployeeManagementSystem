package com.hrm.employeemanagement.application.dto.notification;

import java.util.List;

import com.hrm.employeemanagement.domain.notification.NotificationLevel;

public record CreateNotificationEventCommand(
        String eventType,
        NotificationLevel level,
        String title,
        String message,
        String relatedEntityType,
        String relatedEntityId,
        String sourceEventKey,
        List<Long> recipientUserIds
) {
    public CreateNotificationEventCommand {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType không được để trống");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title không được để trống");
        }
        if (sourceEventKey == null || sourceEventKey.isBlank()) {
            throw new IllegalArgumentException("sourceEventKey không được để trống");
        }
        if (level == null) {
            level = NotificationLevel.THAP;
        }
        if (recipientUserIds == null) {
            recipientUserIds = List.of();
        }
    }
}
