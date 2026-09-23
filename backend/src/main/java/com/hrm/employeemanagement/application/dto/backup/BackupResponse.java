package com.hrm.employeemanagement.application.dto.backup;

import com.hrm.employeemanagement.domain.backup.Backup;
import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;

import java.time.LocalDateTime;

public class BackupResponse {
    private Long id;
    private String backupCode;
    private String title;
    private String description;
    private BackupType backupType;
    private String backupTypeLabel;
    private String fileName;
    private long fileSizeBytes;
    private String formattedFileSize;
    private String checksum;
    private BackupStatus status;
    private String statusDescription;
    private boolean isAutomatic;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public static BackupResponse fromDomain(Backup backup) {
        if (backup == null) return null;
        BackupResponse res = new BackupResponse();
        res.id = backup.getId();
        res.backupCode = backup.getBackupCode();
        res.title = backup.getTitle();
        res.description = backup.getDescription();
        res.backupType = backup.getBackupType();
        res.backupTypeLabel = backup.getBackupType() != null ? backup.getBackupType().getLabel() : "FULL";
        res.fileName = backup.getFileName();
        res.fileSizeBytes = backup.getFileSizeBytes();
        res.formattedFileSize = formatFileSize(backup.getFileSizeBytes());
        res.checksum = backup.getChecksum();
        res.status = backup.getStatus();
        res.statusDescription = backup.getStatus() != null ? backup.getStatus().getDescription() : "";
        res.isAutomatic = backup.isAutomatic();
        res.createdBy = backup.getCreatedBy();
        res.createdByName = backup.getCreatedByName();
        res.createdAt = backup.getCreatedAt();
        res.completedAt = backup.getCompletedAt();
        res.errorMessage = backup.getErrorMessage();
        return res;
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public Long getId() {
        return id;
    }

    public String getBackupCode() {
        return backupCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public String getBackupTypeLabel() {
        return backupTypeLabel;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFormattedFileSize() {
        return formattedFileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public boolean isAutomatic() {
        return isAutomatic;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
