package com.hrm.employeemanagement.domain.backup;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class BackupSchedule {
    private Long id;
    private boolean isEnabled;
    private BackupFrequency frequency;
    private String scheduledTime; // "02:00"
    private String dayOfWeek;     // "MONDAY", "TUESDAY", ...
    private BackupType backupType;
    private int retentionDays;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public BackupSchedule(
            Long id,
            boolean isEnabled,
            BackupFrequency frequency,
            String scheduledTime,
            String dayOfWeek,
            BackupType backupType,
            int retentionDays,
            LocalDateTime lastRunAt,
            LocalDateTime nextRunAt,
            Long updatedBy,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.isEnabled = isEnabled;
        this.frequency = frequency != null ? frequency : BackupFrequency.DAILY;
        this.scheduledTime = scheduledTime != null ? scheduledTime : "02:00";
        this.dayOfWeek = dayOfWeek != null ? dayOfWeek : "MONDAY";
        this.backupType = backupType != null ? backupType : BackupType.FULL;
        this.retentionDays = retentionDays > 0 ? retentionDays : 30;
        this.lastRunAt = lastRunAt;
        this.nextRunAt = nextRunAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public static BackupSchedule createDefault() {
        BackupSchedule schedule = new BackupSchedule(
                1L,
                false,
                BackupFrequency.DAILY,
                "02:00",
                "MONDAY",
                BackupType.FULL,
                30,
                null,
                null,
                null,
                LocalDateTime.now()
        );
        schedule.recalculateNextRun(LocalDateTime.now());
        return schedule;
    }

    public void update(
            boolean isEnabled,
            BackupFrequency frequency,
            String scheduledTime,
            String dayOfWeek,
            BackupType backupType,
            int retentionDays,
            Long updatedBy
    ) {
        this.isEnabled = isEnabled;
        this.frequency = frequency != null ? frequency : BackupFrequency.DAILY;
        this.scheduledTime = scheduledTime != null ? scheduledTime : "02:00";
        this.dayOfWeek = dayOfWeek != null ? dayOfWeek : "MONDAY";
        this.backupType = backupType != null ? backupType : BackupType.FULL;
        this.retentionDays = retentionDays > 0 ? retentionDays : 30;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        recalculateNextRun(LocalDateTime.now());
    }

    public void recordRunSuccess(LocalDateTime runTime) {
        this.lastRunAt = runTime;
        recalculateNextRun(runTime);
    }

    public void recalculateNextRun(LocalDateTime fromTime) {
        if (!isEnabled) {
            this.nextRunAt = null;
            return;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(scheduledTime);
        } catch (Exception e) {
            time = LocalTime.of(2, 0);
        }

        LocalDate today = fromTime.toLocalDate();
        LocalDateTime candidate = today.atTime(time);

        if (frequency == BackupFrequency.DAILY) {
            if (candidate.isAfter(fromTime)) {
                this.nextRunAt = candidate;
            } else {
                this.nextRunAt = candidate.plusDays(1);
            }
        } else if (frequency == BackupFrequency.WEEKLY) {
            DayOfWeek targetDay;
            try {
                targetDay = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            } catch (Exception e) {
                targetDay = DayOfWeek.MONDAY;
            }

            LocalDateTime nextDayTime = fromTime.with(TemporalAdjusters.nextOrSame(targetDay)).toLocalDate().atTime(time);
            if (nextDayTime.isAfter(fromTime)) {
                this.nextRunAt = nextDayTime;
            } else {
                this.nextRunAt = fromTime.with(TemporalAdjusters.next(targetDay)).toLocalDate().atTime(time);
            }
        }
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

    public BackupFrequency getFrequency() {
        return frequency;
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
