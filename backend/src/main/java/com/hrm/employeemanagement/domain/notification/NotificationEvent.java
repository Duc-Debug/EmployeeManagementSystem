package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Aggregate đại diện cho sự kiện thông báo (notification_events).
 * Một business event duy nhất ứng với 1 sourceEventKey theo quy tắc QTN-19.
 */
public class NotificationEvent {
    private final NotificationEventId id;
    private final String eventType;
    private final NotificationLevel level;
    private final String title;
    private final String message;
    private final String relatedEntityType;
    private final String relatedEntityId;
    private final String sourceEventKey;
    private final LocalDateTime createdAt;

    public NotificationEvent(
            NotificationEventId id,
            String eventType,
            NotificationLevel level,
            String title,
            String message,
            String relatedEntityType,
            String relatedEntityId,
            String sourceEventKey,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.eventType = Objects.requireNonNull(eventType, "eventType không được để trống").trim();
        this.level = level != null ? level : NotificationLevel.THAP;
        this.title = Objects.requireNonNull(title, "title không được để trống").trim();
        this.message = message != null ? message.trim() : "";
        this.relatedEntityType = relatedEntityType != null ? relatedEntityType.trim() : null;
        this.relatedEntityId = relatedEntityId != null ? relatedEntityId.trim() : null;
        this.sourceEventKey = Objects.requireNonNull(sourceEventKey, "sourceEventKey không được để trống").trim();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static NotificationEvent create(
            String eventType,
            NotificationLevel level,
            String title,
            String message,
            String relatedEntityType,
            String relatedEntityId,
            String sourceEventKey
    ) {
        return new NotificationEvent(
                null,
                eventType,
                level,
                title,
                message,
                relatedEntityType,
                relatedEntityId,
                sourceEventKey,
                LocalDateTime.now()
        );
    }

    public NotificationEventId getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getSourceEventKey() {
        return sourceEventKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
