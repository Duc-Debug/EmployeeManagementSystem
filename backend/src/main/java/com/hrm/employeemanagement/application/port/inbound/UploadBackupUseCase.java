package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.domain.backup.Backup;

import java.io.InputStream;

public interface UploadBackupUseCase {
    Backup uploadBackup(
            String originalFileName,
            String title,
            String description,
            InputStream inputStream,
            long fileSizeBytes,
            Long currentUserId,
            String currentUserEmail,
            String clientIp
    );
}

