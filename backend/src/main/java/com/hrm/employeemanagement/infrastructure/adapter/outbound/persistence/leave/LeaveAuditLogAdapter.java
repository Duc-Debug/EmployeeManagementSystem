package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave;

import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class LeaveAuditLogAdapter implements SaveLeaveAuditLogPort {

    private final SpringDataAuditLogRepository auditLogRepository;

    public LeaveAuditLogAdapter(SpringDataAuditLogRepository auditLogRepository) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
    }

    @Override
    public void recordAudit(Long userId, String action, String description) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity(
                null,
                userId,
                action,
                "leave_requests",
                null,
                LocalDateTime.now(),
                null,
                description
        );
        auditLogRepository.save(entity);
    }
}
