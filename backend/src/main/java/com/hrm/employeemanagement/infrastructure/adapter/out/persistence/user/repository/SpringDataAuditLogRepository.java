package com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository;

import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {
}
