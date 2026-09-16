package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictPort;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictRepository;

@Component
public class ScheduleConflictPersistenceAdapter implements LoadScheduleConflictPort, SaveScheduleConflictPort {

    private final SpringDataScheduleConflictRepository repository;
    private final TransactionalConflictSaveHelper transactionalHelper;

    public ScheduleConflictPersistenceAdapter(
            SpringDataScheduleConflictRepository repository,
            TransactionalConflictSaveHelper transactionalHelper
    ) {
        this.repository = repository;
        this.transactionalHelper = transactionalHelper;
    }

    @Override
    public Optional<ScheduleConflict> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ScheduleConflict> findConflicts(
            Integer yearNumber,
            Integer startWeek,
            Integer endWeek,
            Long employeeId,
            ConflictType conflictType,
            ScheduleConflictStatus status
    ) {
        return repository.findConflicts(yearNumber, startWeek, endWeek, employeeId, conflictType, status)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ScheduleConflict> findExistingConflict(
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            ConflictType conflictType
    ) {
        return repository.findFirstByEmployeeIdAndYearNumberAndWeekNumberAndConflictType(
                employeeId, yearNumber, weekNumber, conflictType
        ).map(this::toDomain);
    }

    @Override
    public ScheduleConflict save(ScheduleConflict conflict) {
        ScheduleConflictJpaEntity entity = toEntity(conflict);
        try {
            ScheduleConflictJpaEntity saved = transactionalHelper.saveAndFlushRequiresNew(entity);
            return toDomain(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (!isUniqueKeyViolation(e)) {
                throw e;
            }
            ScheduleConflictJpaEntity updated = transactionalHelper.updateExistingRequiresNew(
                    conflict.getEmployeeId(), conflict.getYearNumber(), conflict.getWeekNumber(), conflict.getConflictType(),
                    entity
            );
            return toDomain(updated);
        }
    }

    private boolean isUniqueKeyViolation(org.springframework.dao.DataIntegrityViolationException e) {
        Throwable current = e;

        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if ("uk_schedule_conflict_existing".equalsIgnoreCase(constraintName)) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    @Override
    public List<ScheduleConflict> saveAll(List<ScheduleConflict> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<ScheduleConflictJpaEntity> entities = conflicts.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        try {
            List<ScheduleConflictJpaEntity> savedEntities = transactionalHelper.saveAllAndFlushRequiresNew(entities);
            return savedEntities.stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (!isUniqueKeyViolation(e)) {
                throw e;
            }
            // Fallback: Khi xảy ra race condition UNIQUE collision trong batch, fallback xử lý an toàn từng record
            return conflicts.stream()
                    .map(this::save)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private ScheduleConflict toDomain(ScheduleConflictJpaEntity entity) {
        if (entity == null) return null;
        return new ScheduleConflict(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getYearNumber(),
                entity.getWeekNumber(),
                entity.getConflictType(),
                entity.getProjectIds(),
                entity.getProjectNames(),
                entity.getLeaveRequestId(),
                entity.getLeaveInfo(),
                entity.getTotalAllocatedHours(),
                entity.getNetAvailableHours(),
                entity.getExcessHours(),
                entity.getStatus(),
                entity.getDetails(),
                entity.getNotifiedAt(),
                entity.getNotifiedBy(),
                entity.getAssignedHandlerId(),
                entity.getResolutionNote(),
                entity.getIsRecurrent(),
                entity.getRecurrentNote(),
                entity.getResolvedAt(),
                entity.getResolvedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private ScheduleConflictJpaEntity toEntity(ScheduleConflict domain) {
        if (domain == null) return null;
        ScheduleConflictJpaEntity entity = new ScheduleConflictJpaEntity();
        entity.setId(domain.getId());
        entity.setEmployeeId(domain.getEmployeeId());
        entity.setYearNumber(domain.getYearNumber());
        entity.setWeekNumber(domain.getWeekNumber());
        entity.setConflictType(domain.getConflictType());
        entity.setProjectIds(domain.getProjectIds());
        entity.setProjectNames(domain.getProjectNames());
        entity.setLeaveRequestId(domain.getLeaveRequestId());
        entity.setLeaveInfo(domain.getLeaveInfo());
        entity.setTotalAllocatedHours(domain.getTotalAllocatedHours());
        entity.setNetAvailableHours(domain.getNetAvailableHours());
        entity.setExcessHours(domain.getExcessHours());
        entity.setStatus(domain.getStatus());
        entity.setDetails(domain.getDetails());
        entity.setNotifiedAt(domain.getNotifiedAt());
        entity.setNotifiedBy(domain.getNotifiedBy());
        entity.setAssignedHandlerId(domain.getAssignedHandlerId());
        entity.setResolutionNote(domain.getResolutionNote());
        entity.setIsRecurrent(domain.getIsRecurrent());
        entity.setRecurrentNote(domain.getRecurrentNote());
        entity.setResolvedAt(domain.getResolvedAt());
        entity.setResolvedBy(domain.getResolvedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}
