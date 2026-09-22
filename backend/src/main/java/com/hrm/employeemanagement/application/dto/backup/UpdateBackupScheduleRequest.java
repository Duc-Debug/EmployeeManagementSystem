package com.hrm.employeemanagement.application.dto.backup;

import com.hrm.employeemanagement.domain.backup.BackupFrequency;
import com.hrm.employeemanagement.domain.backup.BackupType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UpdateBackupScheduleRequest {
    private boolean isEnabled;

    @NotNull(message = "Tần suất không được để trống")
    private BackupFrequency frequency = BackupFrequency.DAILY;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ sao lưu phải đúng định dạng HH:mm (ví dụ 02:00)")
    private String scheduledTime = "02:00";

    private String dayOfWeek = "MONDAY";

    private BackupType backupType = BackupType.FULL;

    @Min(value = 1, message = "Thời gian lưu trữ tối thiểu là 1 ngày")
    @Max(value = 365, message = "Thời gian lưu trữ tối đa là 365 ngày")
    private int retentionDays = 30;

    public UpdateBackupScheduleRequest() {
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public BackupFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(BackupFrequency frequency) {
        this.frequency = frequency;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
