package com.hrm.employeemanagement.application.dto.backup;

import com.hrm.employeemanagement.domain.backup.BackupFrequency;
import com.hrm.employeemanagement.domain.backup.BackupSchedule;
import com.hrm.employeemanagement.domain.backup.BackupType;

import java.time.LocalDateTime;

public class BackupScheduleResponse {
    private Long id;
    private boolean isEnabled;
    private BackupFrequency frequency;
    private String frequencyLabel;
    private String scheduledTime;
    private String dayOfWeek;
    private BackupType backupType;
    private String backupTypeLabel;
    private int retentionDays;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public static BackupScheduleResponse fromDomain(BackupSchedule s) {
        if (s == null) return null;
        BackupScheduleResponse res = new BackupScheduleResponse();
        res.id = s.getId();
        res.isEnabled = s.isEnabled();
        res.frequency = s.getFrequency();
        res.frequencyLabel = s.getFrequency() != null ? s.getFrequency().getLabel() : "DAILY";
        res.scheduledTime = s.getScheduledTime();
        res.dayOfWeek = s.getDayOfWeek();
        res.backupType = s.getBackupType();
        res.backupTypeLabel = s.getBackupType() != null ? s.getBackupType().getLabel() : "FULL";
        res.retentionDays = s.getRetentionDays();
        res.lastRunAt = s.getLastRunAt();
        res.nextRunAt = s.getNextRunAt();
        res.updatedBy = s.getUpdatedBy();
        res.updatedAt = s.getUpdatedAt();
        return res;
    }

    public Long getId() {
        return id;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public BackupFrequency getFrequency() {
        return frequency;
    }

    public String getFrequencyLabel() {
        return frequencyLabel;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public String getBackupTypeLabel() {
        return backupTypeLabel;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
