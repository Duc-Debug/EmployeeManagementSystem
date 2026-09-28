package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.UpdateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;

public interface UpdateWorkLogUseCase {
    WorkLogResult updateWorkLog(UpdateWorkLogCommand command);
}
