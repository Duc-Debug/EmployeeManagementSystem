package com.hrm.employeemanagement.application.port.outbound.allocation.period;

import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanningPeriod;

public interface SaveAllocationPlanningPeriodPort {

    AllocationPlanningPeriod save(AllocationPlanningPeriod period);
}
