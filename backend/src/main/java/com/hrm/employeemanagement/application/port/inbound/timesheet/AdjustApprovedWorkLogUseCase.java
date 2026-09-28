package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogResult;

public interface AdjustApprovedWorkLogUseCase {
    AdjustApprovedWorkLogResult adjustApprovedWorkLog(AdjustApprovedWorkLogCommand command);
}

