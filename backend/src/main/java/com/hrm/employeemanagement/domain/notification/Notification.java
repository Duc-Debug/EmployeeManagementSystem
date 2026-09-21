package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.user.UserId;

public class Notification {
    private final NotificationId id;
    private final UserId recipientId;
    private final UserId senderId;
    private final NotificationType type;
    private final String targetType;
    private final Long targetId;
    private final String title;
    private final String content;
    private boolean isRead;
    private final LocalDateTime createdAt;
    private final LocalDateTime availableAt;

    public Notification(
            NotificationId id,
            UserId recipientId,
            UserId senderId,
            NotificationType type,
            String targetType,
            Long targetId,
            String title,
            String content,
            boolean isRead,
            LocalDateTime createdAt) {
        this(id, recipientId, senderId, type, targetType, targetId, title, content, isRead,
                createdAt, createdAt);
    }

    public Notification(
            NotificationId id,
            UserId recipientId,
            UserId senderId,
            NotificationType type,
            String targetType,
            Long targetId,
            String title,
            String content,
            boolean isRead,
            LocalDateTime createdAt,
            LocalDateTime availableAt) {
        this.id = id;
        this.recipientId = Objects.requireNonNull(recipientId, "RecipientId không được null");
        this.senderId = senderId;
        this.type = type != null ? type : NotificationType.TASK_MENTION;
        this.targetType = targetType != null ? targetType : "TASK";
        this.targetId = Objects.requireNonNull(targetId, "TargetId không được null");
        this.title = Objects.requireNonNull(title, "Tiêu đề thông báo không được để trống").trim();
        this.content = content != null ? content.trim() : null;
        this.isRead = isRead;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.availableAt = availableAt != null ? availableAt : this.createdAt;
    }

    public static Notification create(
            UserId recipientId,
            UserId senderId,
            NotificationType type,
            String targetType,
            Long targetId,
            String title,
            String content) {
        return new Notification(
                null,
                recipientId,
                senderId,
                type,
                targetType,
                targetId,
                title,
                content,
                false,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public NotificationId getId() {
        return id;
    }

    public UserId getRecipientId() {
        return recipientId;
    }

    public UserId getSenderId() {
        return senderId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    public Notification scheduledFor(LocalDateTime releaseAt) {
        return new Notification(id, recipientId, senderId, type, targetType, targetId, title, content,
                isRead, createdAt, releaseAt);
    }
}

