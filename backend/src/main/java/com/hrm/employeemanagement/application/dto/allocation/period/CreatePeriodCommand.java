package com.hrm.employeemanagement.application.dto.allocation.period;

import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodType;

public record CreatePeriodCommand(
        String name,
        AllocationPeriodType periodType,
        Integer year,
        Integer startWeek,
        Integer endWeek
) {
}
