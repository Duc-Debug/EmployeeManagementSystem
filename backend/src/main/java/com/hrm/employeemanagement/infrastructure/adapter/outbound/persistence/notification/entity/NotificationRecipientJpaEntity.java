package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "notification_recipients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_recipient_event_user",
                        columnNames = {"notification_event_id", "recipient_user_id"}
                )
        }
)
public class NotificationRecipientJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_event_id", nullable = false)
    private Long notificationEventId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    public NotificationRecipientJpaEntity() {
    }

    public NotificationRecipientJpaEntity(
            Long id,
            Long notificationEventId,
            Long recipientUserId,
            boolean isRead,
            LocalDateTime readAt,
            boolean isDeleted,
            LocalDateTime deletedAt,
            LocalDateTime createdAt
    ) {
        this(id, notificationEventId, recipientUserId, isRead, readAt, isDeleted, deletedAt,
                createdAt, createdAt);
    }

    public NotificationRecipientJpaEntity(
            Long id, Long notificationEventId, Long recipientUserId, boolean isRead,
            LocalDateTime readAt, boolean isDeleted, LocalDateTime deletedAt,
            LocalDateTime createdAt, LocalDateTime availableAt
    ) {
        this.id = id;
        this.notificationEventId = notificationEventId;
        this.recipientUserId = recipientUserId;
        this.isRead = isRead;
        this.readAt = readAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.availableAt = availableAt != null ? availableAt : this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNotificationEventId() {
        return notificationEventId;
    }

    public void setNotificationEventId(Long notificationEventId) {
        this.notificationEventId = notificationEventId;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAvailableAt() { return availableAt; }
    public void setAvailableAt(LocalDateTime availableAt) { this.availableAt = availableAt; }
}
