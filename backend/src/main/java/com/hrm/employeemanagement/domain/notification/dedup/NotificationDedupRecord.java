package com.hrm.employeemanagement.domain.notification.dedup;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity đại diện cho bản ghi chống gửi trùng thông báo (Active/Status-based Dedup Record).
 * Tuân thủ QTN-19 và Acceptance Criteria TC-01, TC-02:
 * - Khi ACTIVE: activeDedupKey = dedupKey, bảo vệ unique constraint.
 * - Khi RESOLVED/INACTIVE: activeDedupKey = null, cho phép tạo bản ghi mới nếu sự kiện tái diễn (TC-02).
 */
public class NotificationDedupRecord {

    private Long id;
    private final String dedupKey;
    private String activeDedupKey;
    private final String eventType;
    private final String targetEntityType;
    private final String targetEntityId;
    private final String yearWeek;
    private final Long recipientUserId;
    private DedupRecordStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime expiresAt;

    public NotificationDedupRecord(
            Long id,
            String dedupKey,
            String activeDedupKey,
            String eventType,
            String targetEntityType,
            String targetEntityId,
            String yearWeek,
            Long recipientUserId,
            DedupRecordStatus status,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt,
            LocalDateTime expiresAt
    ) {
        this.id = id;
        this.dedupKey = Objects.requireNonNull(dedupKey, "dedupKey must not be null");
        this.activeDedupKey = activeDedupKey;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.targetEntityType = Objects.requireNonNull(targetEntityType, "targetEntityType must not be null");
        this.targetEntityId = Objects.requireNonNull(targetEntityId, "targetEntityId must not be null");
        this.yearWeek = Objects.requireNonNull(yearWeek, "yearWeek must not be null");
        this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.resolvedAt = resolvedAt;
        this.expiresAt = expiresAt;
    }

    public static NotificationDedupRecord createActive(
            String dedupKey,
            String eventType,
            String targetEntityType,
            String targetEntityId,
            String yearWeek,
            Long recipientUserId,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        return new NotificationDedupRecord(
                null,
                dedupKey,
                dedupKey,
                eventType,
                targetEntityType,
                targetEntityId,
                yearWeek,
                recipientUserId,
                DedupRecordStatus.ACTIVE,
                createdAt,
                null,
                expiresAt
        );
    }

    public void resolve(LocalDateTime resolvedAt) {
        this.status = DedupRecordStatus.INACTIVE;
        this.activeDedupKey = null;
        this.resolvedAt = resolvedAt != null ? resolvedAt : LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == DedupRecordStatus.ACTIVE;
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

    public String getActiveDedupKey() {
        return activeDedupKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public String getYearWeek() {
        return yearWeek;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public DedupRecordStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
