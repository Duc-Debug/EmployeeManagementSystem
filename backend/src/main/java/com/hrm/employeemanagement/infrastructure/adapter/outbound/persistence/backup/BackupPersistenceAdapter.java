package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup;

import com.hrm.employeemanagement.application.port.outbound.BackupRepositoryPort;
import com.hrm.employeemanagement.domain.backup.Backup;
import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity.SystemBackupJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.repository.SpringDataSystemBackupRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BackupPersistenceAdapter implements BackupRepositoryPort {

    private final SpringDataSystemBackupRepository backupRepository;

    public BackupPersistenceAdapter(SpringDataSystemBackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    @Override
    public Backup save(Backup backup) {
        SystemBackupJpaEntity entity = SystemBackupJpaEntity.fromDomain(backup);
        SystemBackupJpaEntity saved = backupRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Backup> findById(Long id) {
        return backupRepository.findById(id).map(SystemBackupJpaEntity::toDomain);
    }

    @Override
    public Optional<Backup> findByBackupCode(String backupCode) {
        return backupRepository.findByBackupCode(backupCode).map(SystemBackupJpaEntity::toDomain);
    }

    @Override
    public List<Backup> findAll() {
        return backupRepository.findAll().stream()
                .map(SystemBackupJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Backup> findFiltered(BackupType type, BackupStatus status, String search) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return backupRepository.findFiltered(type, status, trimmedSearch).stream()
                .map(SystemBackupJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Backup> findExpiredBackups(java.time.LocalDateTime cutoffDate) {
        return backupRepository.findExpiredBackups(cutoffDate).stream()
                .map(SystemBackupJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        backupRepository.deleteById(id);
    }

    @Override
    public long count() {
        return backupRepository.count();
    }

    @Override
    public Optional<Backup> findLatestCompleted() {
        return backupRepository.findLatestCompleted().map(SystemBackupJpaEntity::toDomain);
    }

    @Override
    public long sumTotalFileSizeBytes() {
        return backupRepository.sumTotalFileSizeBytes();
    }
}
