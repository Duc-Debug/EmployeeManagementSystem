package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period;

import java.util.List;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodType;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshot;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshotItem;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanningPeriod;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanSnapshotItemJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanSnapshotJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanningPeriodJpaEntity;

public class AllocationPeriodPersistenceMapper {

    public static AllocationPlanningPeriod toDomainPeriod(AllocationPlanningPeriodJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AllocationPlanningPeriod(
                entity.getId(),
                entity.getName(),
                entity.getPeriodType() != null ? AllocationPeriodType.valueOf(entity.getPeriodType()) : AllocationPeriodType.QUARTER,
                entity.getYear(),
                entity.getStartWeek(),
                entity.getEndWeek(),
                entity.getStatus() != null ? AllocationPeriodStatus.valueOf(entity.getStatus()) : AllocationPeriodStatus.OPEN,
                entity.getLockedBy(),
                entity.getLockedAt(),
                entity.getUnlockedBy(),
                entity.getUnlockedAt(),
                entity.getUnlockReason(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public static AllocationPlanningPeriodJpaEntity toEntityPeriod(AllocationPlanningPeriod domain) {
        if (domain == null) {
            return null;
        }
        return new AllocationPlanningPeriodJpaEntity(
                domain.getId(),
                domain.getName(),
                domain.getPeriodType().name(),
                domain.getYear(),
                domain.getStartWeek(),
                domain.getEndWeek(),
                domain.getStatus().name(),
                domain.getLockedBy(),
                domain.getLockedAt(),
                domain.getUnlockedBy(),
                domain.getUnlockedAt(),
                domain.getUnlockReason(),
                domain.getCreatedBy(),
                domain.getVersion()
        );
    }

    public static AllocationPlanSnapshot toDomainSnapshot(AllocationPlanSnapshotJpaEntity entity, List<AllocationPlanSnapshotItemJpaEntity> itemEntities) {
        if (entity == null) {
            return null;
        }
        List<AllocationPlanSnapshotItem> items = itemEntities != null
                ? itemEntities.stream().map(AllocationPeriodPersistenceMapper::toDomainSnapshotItem).toList()
                : List.of();

        return new AllocationPlanSnapshot(
                entity.getId(),
                entity.getPeriodId(),
                entity.getSnapshotVersion(),
                entity.getTotalAllocations(),
                entity.getTotalAllocatedHours(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                items
        );
    }

    public static AllocationPlanSnapshotItem toDomainSnapshotItem(AllocationPlanSnapshotItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AllocationPlanSnapshotItem(
                entity.getId(),
                entity.getSnapshotId(),
                entity.getOriginalAllocationId(),
                entity.getEmployeeId(),
                entity.getProjectId(),
                entity.getYear(),
                entity.getWeekNumber(),
                entity.getAllocatedHours(),
                entity.getAllocationPercentage(),
                entity.getIsOverloaded() != null ? entity.getIsOverloaded() : false,
                entity.getOverloadReason()
        );
    }

    public static WeeklyProjectAllocation toDomainAllocation(WeeklyProjectAllocationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new WeeklyProjectAllocation(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getProjectId(),
                YearWeek.of(entity.getYear(), entity.getWeekNumber()),
                entity.getAllocatedHours(),
                entity.getAllocationPercentage(),
                entity.getIsOverloaded() != null ? entity.getIsOverloaded() : false,
                entity.getOverloadReason(),
                entity.getOverloadApprovedBy(),
                entity.getOverloadApprovedAt(),
                entity.getVersion()
        );
    }
}
