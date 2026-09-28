package com.hrm.employeemanagement.application.port.inbound.allocation.period;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPeriodResult;
import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPlanSnapshotResult;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;

public interface GetAllocationPeriodsUseCase {

    List<AllocationPeriodResult> getPeriods(Integer year, AllocationPeriodStatus status);

    AllocationPeriodResult getPeriodById(Long id);

    List<AllocationPlanSnapshotResult> getPeriodSnapshots(Long periodId);

    AllocationPlanSnapshotResult getSnapshotDetail(Long periodId, Long snapshotId);
}
