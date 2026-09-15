package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {

    List<AuditLogJpaEntity> findByTableNameAndRecordIdOrderByCreatedAtDesc(String tableName, Long recordId);

    List<AuditLogJpaEntity> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("delete from AuditLogJpaEntity auditLog where auditLog.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
