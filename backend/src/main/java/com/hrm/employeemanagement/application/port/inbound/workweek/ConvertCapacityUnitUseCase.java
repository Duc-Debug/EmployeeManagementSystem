package com.hrm.employeemanagement.application.port.inbound.workweek;

import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionCommand;
import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionResult;

public interface ConvertCapacityUnitUseCase {
    CapacityConversionResult execute(CapacityConversionCommand command);
}

