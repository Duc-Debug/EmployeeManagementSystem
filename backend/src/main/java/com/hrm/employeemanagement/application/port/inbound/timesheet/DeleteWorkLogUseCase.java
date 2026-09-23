package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.DeleteWorkLogCommand;

public interface DeleteWorkLogUseCase {
    void deleteWorkLog(DeleteWorkLogCommand command);
}
