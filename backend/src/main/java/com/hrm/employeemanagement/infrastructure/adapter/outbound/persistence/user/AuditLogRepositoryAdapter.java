package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryAdapter implements SaveAuditLogPort {

    private final SpringDataAuditLogRepository springDataAuditLogRepository;
    private final UserPersistenceMapper mapper;

    public AuditLogRepositoryAdapter(SpringDataAuditLogRepository springDataAuditLogRepository, UserPersistenceMapper mapper) {
        this.springDataAuditLogRepository = springDataAuditLogRepository;
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = mapper.toJpaEntity(auditLog);
        AuditLogJpaEntity saved = springDataAuditLogRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
