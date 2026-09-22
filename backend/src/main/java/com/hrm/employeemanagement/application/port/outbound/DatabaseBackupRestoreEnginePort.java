package com.hrm.employeemanagement.application.port.outbound;

import com.hrm.employeemanagement.domain.backup.BackupType;

import java.io.File;

public interface DatabaseBackupRestoreEnginePort {
    File performBackup(File targetFile, BackupType backupType) throws Exception;
    void performRestore(File backupFile, BackupType backupType) throws Exception;
}
