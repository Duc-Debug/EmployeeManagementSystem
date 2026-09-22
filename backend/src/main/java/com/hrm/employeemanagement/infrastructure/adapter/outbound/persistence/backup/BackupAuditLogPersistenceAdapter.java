package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup;

import com.hrm.employeemanagement.application.port.outbound.BackupAuditLogPort;
import com.hrm.employeemanagement.domain.backup.BackupAuditLog;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity.BackupAuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.repository.SpringDataBackupAuditLogRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BackupAuditLogPersistenceAdapter implements BackupAuditLogPort {

    private final SpringDataBackupAuditLogRepository auditLogRepository;

    public BackupAuditLogPersistenceAdapter(SpringDataBackupAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public BackupAuditLog save(BackupAuditLog log) {
        BackupAuditLogJpaEntity entity = BackupAuditLogJpaEntity.fromDomain(log);
        BackupAuditLogJpaEntity saved = auditLogRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<BackupAuditLog> findAll() {
        return auditLogRepository.findAll().stream()
                .map(BackupAuditLogJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<BackupAuditLog> findRecent(int limit) {
        return auditLogRepository.findRecentLogs().stream()
                .limit(limit > 0 ? limit : 100)
                .map(BackupAuditLogJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}
