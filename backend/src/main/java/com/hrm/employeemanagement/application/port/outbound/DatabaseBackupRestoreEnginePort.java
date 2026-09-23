package com.hrm.employeemanagement.application.port.outbound;

import com.hrm.employeemanagement.domain.backup.BackupType;

import java.io.File;
import java.util.List;

public interface DatabaseBackupRestoreEnginePort {
    File performBackup(File targetFile, BackupType backupType) throws Exception;
    void performRestore(File backupFile, BackupType backupType) throws Exception;
    List<String> getSupportedTables();
    List<String> getSupportedTables(BackupType backupType);

    default boolean isSupportedTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) return false;
        List<String> tables = getSupportedTables();
        if (tables == null) return false;
        return tables.stream().anyMatch(t -> t.equalsIgnoreCase(tableName.trim()));
    }
}
