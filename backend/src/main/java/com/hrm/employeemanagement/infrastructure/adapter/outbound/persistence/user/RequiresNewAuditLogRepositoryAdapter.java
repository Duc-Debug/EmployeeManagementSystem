package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;

@Component
public class RequiresNewAuditLogRepositoryAdapter
        implements SaveAuditLogInNewTransactionPort {

    private final SpringDataAuditLogRepository auditLogRepository;
    private final UserPersistenceMapper mapper;

    public RequiresNewAuditLogRepositoryAdapter(
            SpringDataAuditLogRepository auditLogRepository,
            UserPersistenceMapper mapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity =
                mapper.toJpaEntity(auditLog);

        AuditLogJpaEntity saved =
                auditLogRepository.saveAndFlush(entity);

        return mapper.toDomain(saved);
    }
}
