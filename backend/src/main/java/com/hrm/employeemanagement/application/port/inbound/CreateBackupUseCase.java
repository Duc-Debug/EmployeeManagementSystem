package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.application.dto.backup.CreateBackupRequest;
import com.hrm.employeemanagement.domain.backup.Backup;

public interface CreateBackupUseCase {
    Backup createBackup(CreateBackupRequest request, Long currentUserId, String currentUserEmail, String createdByName, String clientIp);
    default Backup createBackup(CreateBackupRequest request, Long currentUserId, String currentUserEmail, String clientIp) {
        return createBackup(request, currentUserId, currentUserEmail, currentUserEmail, clientIp);
    }
    Backup executeAutomaticBackup();
}
