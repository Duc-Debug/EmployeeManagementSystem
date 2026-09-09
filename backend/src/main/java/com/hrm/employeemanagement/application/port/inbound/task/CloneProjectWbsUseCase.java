package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsCommand;
import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsResult;

public interface CloneProjectWbsUseCase {

    CloneProjectWbsResult cloneWbs(CloneProjectWbsCommand command);
}
