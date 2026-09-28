package com.hrm.employeemanagement.application.dto.backup;

public class BackupSummaryResponse {
    private long totalBackups;
    private long totalFileSizeBytes;
    private String formattedTotalSize;
    private String storageLocation;
    private BackupResponse latestCompletedBackup;
    private BackupScheduleResponse schedule;

    public BackupSummaryResponse() {
    }

    public BackupSummaryResponse(
            long totalBackups,
            long totalFileSizeBytes,
            String storageLocation,
            BackupResponse latestCompletedBackup,
            BackupScheduleResponse schedule
    ) {
        this.totalBackups = totalBackups;
        this.totalFileSizeBytes = totalFileSizeBytes;
        this.formattedTotalSize = BackupResponse.formatFileSize(totalFileSizeBytes);
        this.storageLocation = storageLocation;
        this.latestCompletedBackup = latestCompletedBackup;
        this.schedule = schedule;
    }

    public long getTotalBackups() {
        return totalBackups;
    }

    public long getTotalFileSizeBytes() {
        return totalFileSizeBytes;
    }

    public String getFormattedTotalSize() {
        return formattedTotalSize;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public BackupResponse getLatestCompletedBackup() {
        return latestCompletedBackup;
    }

    public BackupScheduleResponse getSchedule() {
        return schedule;
    }
}
