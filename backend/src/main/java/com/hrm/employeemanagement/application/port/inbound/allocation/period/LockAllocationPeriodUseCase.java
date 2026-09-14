package com.hrm.employeemanagement.application.port.inbound.allocation.period;

import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPeriodResult;
import com.hrm.employeemanagement.application.dto.allocation.period.LockPeriodCommand;

public interface LockAllocationPeriodUseCase {

    AllocationPeriodResult lockPeriod(LockPeriodCommand command);
}
