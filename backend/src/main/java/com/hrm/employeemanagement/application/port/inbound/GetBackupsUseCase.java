package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.domain.backup.Backup;
import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;

import java.util.List;

public interface GetBackupsUseCase {
    List<Backup> getBackups(BackupType type, BackupStatus status, String search);
    Backup getBackupById(Long id);
}
