package com.hrm.employeemanagement.application.port.outbound.allocation;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;

public interface SaveWeeklyProjectAllocationPort {

    WeeklyProjectAllocation save(WeeklyProjectAllocation allocation);
}
