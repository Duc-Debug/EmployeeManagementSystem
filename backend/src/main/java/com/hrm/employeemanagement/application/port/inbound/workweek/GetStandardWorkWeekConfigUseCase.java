package com.hrm.employeemanagement.application.port.inbound.workweek;

import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekConfigResult;

public interface GetStandardWorkWeekConfigUseCase {
    StandardWorkWeekConfigResult execute(String scopeType, Long orgUnitId);
}

