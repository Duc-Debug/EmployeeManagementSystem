package com.hrm.employeemanagement.application.port.inbound.workweek;

import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekConfigResult;
import com.hrm.employeemanagement.application.dto.workweek.UpdateStandardWorkWeekCommand;

public interface UpdateStandardWorkWeekConfigUseCase {
    StandardWorkWeekConfigResult execute(UpdateStandardWorkWeekCommand command);
}

