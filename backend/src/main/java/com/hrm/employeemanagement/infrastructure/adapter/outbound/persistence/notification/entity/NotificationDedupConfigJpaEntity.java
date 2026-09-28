package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notification_dedup_configs")
public class NotificationDedupConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    @Column(name = "dedup_window_days", nullable = false)
    private int dedupWindowDays;

    @Column(name = "scan_interval_minutes", nullable = false)
    private int scanIntervalMinutes;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public NotificationDedupConfigJpaEntity() {
    }

    public NotificationDedupConfigJpaEntity(
            Long id,
            boolean isEnabled,
            int dedupWindowDays,
            int scanIntervalMinutes,
            Long updatedBy,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.isEnabled = isEnabled;
        this.dedupWindowDays = dedupWindowDays;
        this.scanIntervalMinutes = scanIntervalMinutes;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public int getDedupWindowDays() {
        return dedupWindowDays;
    }

    public void setDedupWindowDays(int dedupWindowDays) {
        this.dedupWindowDays = dedupWindowDays;
    }

    public int getScanIntervalMinutes() {
        return scanIntervalMinutes;
    }

    public void setScanIntervalMinutes(int scanIntervalMinutes) {
        this.scanIntervalMinutes = scanIntervalMinutes;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
