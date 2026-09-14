package com.hrm.employeemanagement.application.port.inbound.allocation.period;

import com.hrm.employeemanagement.application.dto.allocation.period.PeriodLockCheckResult;

public interface CheckAllocationPeriodLockUseCase {

    PeriodLockCheckResult checkWeekLock(int year, int weekNumber);

    void validateWeekNotLocked(int year, int weekNumber);
}
