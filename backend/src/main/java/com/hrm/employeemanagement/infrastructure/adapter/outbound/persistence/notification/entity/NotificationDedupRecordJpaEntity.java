package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_dedup_records")
public class NotificationDedupRecordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dedup_key", nullable = false, length = 255)
    private String dedupKey;

    @Column(name = "active_dedup_key", unique = true, length = 255)
    private String activeDedupKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "target_entity_type", nullable = false, length = 100)
    private String targetEntityType;

    @Column(name = "target_entity_id", nullable = false, length = 100)
    private String targetEntityId;

    @Column(name = "year_week", nullable = false, length = 20)
    private String yearWeek;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public NotificationDedupRecordJpaEntity() {
    }

    public NotificationDedupRecordJpaEntity(
            Long id,
            String dedupKey,
            String activeDedupKey,
            String eventType,
            String targetEntityType,
            String targetEntityId,
            String yearWeek,
            Long recipientUserId,
            String status,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt,
            LocalDateTime expiresAt
    ) {
        this.id = id;
        this.dedupKey = dedupKey;
        this.activeDedupKey = activeDedupKey;
        this.eventType = eventType;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
        this.yearWeek = yearWeek;
        this.recipientUserId = recipientUserId;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }

    public String getActiveDedupKey() {
        return activeDedupKey;
    }

    public void setActiveDedupKey(String activeDedupKey) {
        this.activeDedupKey = activeDedupKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public void setTargetEntityType(String targetEntityType) {
        this.targetEntityType = targetEntityType;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(String targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getYearWeek() {
        return yearWeek;
    }

    public void setYearWeek(String yearWeek) {
        this.yearWeek = yearWeek;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
