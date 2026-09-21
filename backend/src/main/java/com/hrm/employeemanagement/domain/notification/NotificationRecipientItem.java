package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Domain Entity đại diện cho hộp thư cá nhân của một người nhận (notification_recipients).
 * Quản lý trạng thái đọc và xóa mềm của riêng người nhận đó mà không ảnh hưởng tới người nhận khác.
 */
public class NotificationRecipientItem {
    private final NotificationRecipientId id;
    private final NotificationEventId eventId;
    private final UserId recipientUserId;
    private boolean isRead;
    private LocalDateTime readAt;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime availableAt;

    public NotificationRecipientItem(
            NotificationRecipientId id,
            NotificationEventId eventId,
            UserId recipientUserId,
            boolean isRead,
            LocalDateTime readAt,
            boolean isDeleted,
            LocalDateTime deletedAt,
            LocalDateTime createdAt
    ) {
        this(id, eventId, recipientUserId, isRead, readAt, isDeleted, deletedAt, createdAt, createdAt);
    }

    public NotificationRecipientItem(
            NotificationRecipientId id,
            NotificationEventId eventId,
            UserId recipientUserId,
            boolean isRead,
            LocalDateTime readAt,
            boolean isDeleted,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime availableAt
    ) {
        this.id = id;
        this.eventId = Objects.requireNonNull(eventId, "eventId không được null");
        this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId không được null");
        this.isRead = isRead;
        this.readAt = readAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.availableAt = availableAt != null ? availableAt : this.createdAt;
    }

    public static NotificationRecipientItem create(NotificationEventId eventId, UserId recipientUserId) {
        return new NotificationRecipientItem(
                null,
                eventId,
                recipientUserId,
                false,
                null,
                false,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void markAsRead(LocalDateTime when) {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = when != null ? when : LocalDateTime.now();
        }
    }

    public void softDelete(LocalDateTime when) {
        if (!this.isDeleted) {
            this.isDeleted = true;
            this.deletedAt = when != null ? when : LocalDateTime.now();
        }
    }

    public NotificationRecipientId getId() {
        return id;
    }

    public NotificationEventId getEventId() {
        return eventId;
    }

    public UserId getRecipientUserId() {
        return recipientUserId;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }
}
