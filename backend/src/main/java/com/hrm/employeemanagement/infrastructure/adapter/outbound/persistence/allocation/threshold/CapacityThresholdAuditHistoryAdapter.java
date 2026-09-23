package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdHistoryPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;

@Component
public class CapacityThresholdAuditHistoryAdapter implements LoadCapacityThresholdHistoryPort {

    private final SpringDataAuditLogRepository auditLogRepository;
    private final LoadUserPort loadUserPort;

    public CapacityThresholdAuditHistoryAdapter(
            SpringDataAuditLogRepository auditLogRepository,
            LoadUserPort loadUserPort
    ) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
    }

    @Override
    public List<CapacityThresholdHistoryResult> loadHistory(String tableName, Long recordId) {
        List<AuditLogJpaEntity> logs = auditLogRepository
                .findByTableNameAndRecordIdOrderByCreatedAtDesc(tableName, recordId);

        return logs.stream().map(log -> {
            String userName = null;
            if (log.getUserId() != null) {
                userName = loadUserPort.findById(new UserId(log.getUserId()))
                        .map(User::getUsername)
                        .orElse(null);
            }

            return new CapacityThresholdHistoryResult(
                    log.getId(),
                    log.getUserId(),
                    userName,
                    log.getAction(),
                    log.getOldValue(),
                    log.getNewValue(),
                    log.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }
}
