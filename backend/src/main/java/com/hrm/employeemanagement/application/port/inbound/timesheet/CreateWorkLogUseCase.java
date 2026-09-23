package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.CreateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;

public interface CreateWorkLogUseCase {
    WorkLogResult createWorkLog(CreateWorkLogCommand command);
}
