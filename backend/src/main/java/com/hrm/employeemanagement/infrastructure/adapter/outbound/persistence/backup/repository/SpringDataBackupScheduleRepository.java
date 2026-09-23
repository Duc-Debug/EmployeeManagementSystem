package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity.BackupScheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataBackupScheduleRepository extends JpaRepository<BackupScheduleJpaEntity, Long> {
}
