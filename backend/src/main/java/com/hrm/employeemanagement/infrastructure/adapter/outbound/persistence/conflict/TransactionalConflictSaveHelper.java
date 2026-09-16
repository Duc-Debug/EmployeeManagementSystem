package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictRepository;

@Component
public class TransactionalConflictSaveHelper {

    private final SpringDataScheduleConflictRepository repository;

    public TransactionalConflictSaveHelper(SpringDataScheduleConflictRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduleConflictJpaEntity saveAndFlushRequiresNew(ScheduleConflictJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ScheduleConflictJpaEntity> saveAllAndFlushRequiresNew(List<ScheduleConflictJpaEntity> entities) {
        return repository.saveAllAndFlush(entities);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduleConflictJpaEntity updateExistingRequiresNew(
            Long employeeId, Integer yearNumber, Integer weekNumber, ConflictType conflictType,
            ScheduleConflictJpaEntity updatedData
    ) {
        ScheduleConflictJpaEntity existing = repository
                .findFirstByEmployeeIdAndYearNumberAndWeekNumberAndConflictType(employeeId, yearNumber, weekNumber, conflictType)
                .orElse(null);

        if (existing == null) {
            return repository.saveAndFlush(updatedData);
        }

        // Chỉ cập nhật các trường tính toán từ scan (scan-derived fields)
        existing.setProjectIds(updatedData.getProjectIds());
        existing.setProjectNames(updatedData.getProjectNames());
        existing.setLeaveRequestId(updatedData.getLeaveRequestId());
        existing.setLeaveInfo(updatedData.getLeaveInfo());
        existing.setTotalAllocatedHours(updatedData.getTotalAllocatedHours());
        existing.setNetAvailableHours(updatedData.getNetAvailableHours());
        existing.setExcessHours(updatedData.getExcessHours());
        existing.setDetails(updatedData.getDetails());

        // Giữ nguyên toàn bộ lifecycle fields của existing (status, assignedHandlerId, resolutionNote, resolvedAt, resolvedBy, etc.)
        return repository.saveAndFlush(existing);
    }
}
