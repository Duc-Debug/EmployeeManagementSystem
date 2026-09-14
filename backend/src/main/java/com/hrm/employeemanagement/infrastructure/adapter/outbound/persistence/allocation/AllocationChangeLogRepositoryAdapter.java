package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadAllocationChangeLogPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveAllocationChangeLogPort;
import com.hrm.employeemanagement.domain.allocation.AdjustmentAction;
import com.hrm.employeemanagement.domain.allocation.AllocationChangeLog;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.AllocationChangeLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataAllocationChangeLogRepository;

@Component
public class AllocationChangeLogRepositoryAdapter implements SaveAllocationChangeLogPort, LoadAllocationChangeLogPort {

    private final SpringDataAllocationChangeLogRepository repository;

    public AllocationChangeLogRepositoryAdapter(SpringDataAllocationChangeLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public AllocationChangeLog save(AllocationChangeLog changeLog) {
        Objects.requireNonNull(changeLog, "changeLog must not be null");
        AllocationChangeLogJpaEntity entity = new AllocationChangeLogJpaEntity(
                changeLog.getId(),
                changeLog.getAllocationId(),
                changeLog.getAction().name(),
                changeLog.getOldValue(),
                changeLog.getNewValue(),
                changeLog.getChangedBy(),
                changeLog.getChangedAt(),
                changeLog.getNotifiedPmIds()
        );
        AllocationChangeLogJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<AllocationChangeLog> findByAllocationId(Long allocationId) {
        if (allocationId == null) {
            return List.of();
        }
        return repository.findByAllocationIdOrderByChangedAtDesc(allocationId).stream()
                .map(this::toDomain)
                .toList();
    }

    private AllocationChangeLog toDomain(AllocationChangeLogJpaEntity e) {
        return new AllocationChangeLog(
                e.getId(),
                e.getAllocationId(),
                AdjustmentAction.valueOf(e.getAction()),
                e.getOldValue(),
                e.getNewValue(),
                e.getChangedBy(),
                e.getChangedAt(),
                e.getNotifiedPmIds()
        );
    }
}
