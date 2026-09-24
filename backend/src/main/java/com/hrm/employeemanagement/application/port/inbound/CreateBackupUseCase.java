package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.application.dto.backup.CreateBackupRequest;
import com.hrm.employeemanagement.domain.backup.Backup;

public interface CreateBackupUseCase {
    Backup createBackup(CreateBackupRequest request, Long currentUserId, String currentUserEmail, String clientIp);
    Backup executeAutomaticBackup();
}
