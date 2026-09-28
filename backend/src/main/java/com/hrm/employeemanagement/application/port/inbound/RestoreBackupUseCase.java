package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.application.dto.backup.RestoreBackupRequest;

public interface RestoreBackupUseCase {
    void restoreBackup(Long backupId, RestoreBackupRequest request, Long currentUserId, String currentUserEmail, String clientIp);
}
