package com.hrm.employeemanagement.application.port.outbound.allocation.period;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshot;

public interface LoadAllocationPlanSnapshotPort {

    Optional<AllocationPlanSnapshot> findSnapshotById(Long id);

    Optional<AllocationPlanSnapshot> findLatestByPeriodId(Long periodId);

    List<AllocationPlanSnapshot> findByPeriodId(Long periodId);

    int countSnapshotsByPeriodId(Long periodId);
}
