package com.hrm.employeemanagement.application.port.outbound.allocation.period;

import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshot;

public interface SaveAllocationPlanSnapshotPort {

    AllocationPlanSnapshot save(AllocationPlanSnapshot snapshot);
}
