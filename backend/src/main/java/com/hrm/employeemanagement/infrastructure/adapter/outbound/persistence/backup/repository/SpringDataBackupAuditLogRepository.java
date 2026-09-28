package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity.BackupAuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataBackupAuditLogRepository extends JpaRepository<BackupAuditLogJpaEntity, Long> {

    @Query("SELECT l FROM BackupAuditLogJpaEntity l ORDER BY l.createdAt DESC LIMIT 100")
    List<BackupAuditLogJpaEntity> findRecentLogs();
}
