package com.hrm.employeemanagement.application.port.inbound;

public interface DeleteBackupUseCase {
    void deleteBackup(Long backupId, String reason, Long currentUserId, String currentUserEmail, String clientIp);
}
