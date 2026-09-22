package com.hrm.employeemanagement.application.dto.backup;

import com.hrm.employeemanagement.domain.backup.BackupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateBackupRequest {
    @NotBlank(message = "Tiêu đề bản sao lưu không được để trống")
    @Size(min = 3, max = 255, message = "Tiêu đề bản sao lưu phải từ 3 đến 255 ký tự")
    private String title;

    private String description;

    private BackupType backupType = BackupType.FULL;

    public CreateBackupRequest() {
    }

    public CreateBackupRequest(String title, String description, BackupType backupType) {
        this.title = title;
        this.description = description;
        this.backupType = backupType != null ? backupType : BackupType.FULL;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }
}
