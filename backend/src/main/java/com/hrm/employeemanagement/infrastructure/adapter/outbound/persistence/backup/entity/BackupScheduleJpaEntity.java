package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity;

import com.hrm.employeemanagement.domain.backup.BackupFrequency;
import com.hrm.employeemanagement.domain.backup.BackupSchedule;
import com.hrm.employeemanagement.domain.backup.BackupType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_schedules")
public class BackupScheduleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 32)
    private BackupFrequency frequency;

    @Column(name = "scheduled_time", nullable = false, length = 8)
    private String scheduledTime;

    @Column(name = "day_of_week", length = 16)
    private String dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type", nullable = false, length = 32)
    private BackupType backupType;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BackupScheduleJpaEntity() {
    }

    public static BackupScheduleJpaEntity fromDomain(BackupSchedule domain) {
        if (domain == null) return null;
        BackupScheduleJpaEntity entity = new BackupScheduleJpaEntity();
        entity.id = domain.getId();
        entity.isEnabled = domain.isEnabled();
        entity.frequency = domain.getFrequency();
        entity.scheduledTime = domain.getScheduledTime();
        entity.dayOfWeek = domain.getDayOfWeek();
        entity.backupType = domain.getBackupType();
        entity.retentionDays = domain.getRetentionDays();
        entity.lastRunAt = domain.getLastRunAt();
        entity.nextRunAt = domain.getNextRunAt();
        entity.updatedBy = domain.getUpdatedBy();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }

    public BackupSchedule toDomain() {
        return new BackupSchedule(
                this.id,
                this.isEnabled,
                this.frequency,
                this.scheduledTime,
                this.dayOfWeek,
                this.backupType,
                this.retentionDays,
                this.lastRunAt,
                this.nextRunAt,
                this.updatedBy,
                this.updatedAt
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public BackupFrequency getFrequency() { return frequency; }
    public void setFrequency(BackupFrequency frequency) { this.frequency = frequency; }
    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public BackupType getBackupType() { return backupType; }
    public void setBackupType(BackupType backupType) { this.backupType = backupType; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(LocalDateTime nextRunAt) { this.nextRunAt = nextRunAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
