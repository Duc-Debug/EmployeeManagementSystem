package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.repository;

import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity.SystemBackupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSystemBackupRepository extends JpaRepository<SystemBackupJpaEntity, Long> {

    Optional<SystemBackupJpaEntity> findByBackupCode(String backupCode);

    @Query("SELECT b FROM SystemBackupJpaEntity b WHERE " +
           "(:type IS NULL OR b.backupType = :type) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.backupCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY b.createdAt DESC")
    List<SystemBackupJpaEntity> findFiltered(
            @Param("type") BackupType type,
            @Param("status") BackupStatus status,
            @Param("search") String search
    );

    @Query("SELECT b FROM SystemBackupJpaEntity b WHERE b.status = 'COMPLETED' ORDER BY b.completedAt DESC LIMIT 1")
    Optional<SystemBackupJpaEntity> findLatestCompleted();

    @Query("SELECT COALESCE(SUM(b.fileSizeBytes), 0) FROM SystemBackupJpaEntity b")
    long sumTotalFileSizeBytes();

    @Query("SELECT b FROM SystemBackupJpaEntity b WHERE b.status = 'COMPLETED' AND b.createdAt < :cutoffDate")
    List<SystemBackupJpaEntity> findExpiredBackups(@Param("cutoffDate") LocalDateTime cutoffDate);
}
