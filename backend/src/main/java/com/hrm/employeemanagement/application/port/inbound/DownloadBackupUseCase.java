package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.domain.backup.Backup;

import java.io.InputStream;

public interface DownloadBackupUseCase {
    InputStream downloadBackup(Long backupId, Long currentUserId, String currentUserEmail, String clientIp);
    Backup getBackupForDownload(Long backupId);
}
