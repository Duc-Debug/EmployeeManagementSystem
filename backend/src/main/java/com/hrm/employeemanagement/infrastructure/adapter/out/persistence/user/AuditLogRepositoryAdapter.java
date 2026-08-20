package com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user;

import com.hrm.employeemanagement.domain.model.audit.AuditLog;
import com.hrm.employeemanagement.domain.repository.user.AuditLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataAuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository springDataAuditLogRepository;

    public AuditLogRepositoryAdapter(SpringDataAuditLogRepository springDataAuditLogRepository) {
        this.springDataAuditLogRepository = springDataAuditLogRepository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getAction(),
                auditLog.getTableName(),
                auditLog.getRecordId(),
                auditLog.getCreatedAt()
        );
        AuditLogJpaEntity saved = springDataAuditLogRepository.save(entity);
        return new AuditLog(saved.getId(), saved.getUserId(), saved.getAction(), saved.getTableName(), saved.getRecordId(), saved.getCreatedAt());
    }
}
