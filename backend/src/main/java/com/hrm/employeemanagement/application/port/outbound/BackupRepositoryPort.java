package com.hrm.employeemanagement.application.port.outbound;

import com.hrm.employeemanagement.domain.backup.Backup;
import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;

import java.util.List;
import java.util.Optional;

public interface BackupRepositoryPort {
    Backup save(Backup backup);
    Optional<Backup> findById(Long id);
    Optional<Backup> findByBackupCode(String backupCode);
    List<Backup> findAll();
    List<Backup> findFiltered(BackupType type, BackupStatus status, String search);
    void deleteById(Long id);
    long count();
    Optional<Backup> findLatestCompleted();
    long sumTotalFileSizeBytes();
}
