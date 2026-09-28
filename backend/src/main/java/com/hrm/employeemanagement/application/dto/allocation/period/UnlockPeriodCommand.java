package com.hrm.employeemanagement.application.dto.allocation.period;

public record UnlockPeriodCommand(
        Long periodId,
        String reason
) {
}
