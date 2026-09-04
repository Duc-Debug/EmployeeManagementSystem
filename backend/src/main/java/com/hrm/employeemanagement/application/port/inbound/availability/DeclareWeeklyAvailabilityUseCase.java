package com.hrm.employeemanagement.application.port.inbound.availability;

import com.hrm.employeemanagement.application.dto.availability.DeclareWeeklyAvailabilityCommand;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;

public interface DeclareWeeklyAvailabilityUseCase {
    WeeklyAvailabilityResult execute(DeclareWeeklyAvailabilityCommand command);
}
