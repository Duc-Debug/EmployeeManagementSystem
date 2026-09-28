package com.hrm.employeemanagement.application.port.inbound.availability;

import com.hrm.employeemanagement.application.dto.availability.CalculateWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;

public interface CalculateWeeklyCapacityUseCase {
    WeeklyAvailabilityResult calculate(CalculateWeeklyCapacityQuery query);
}
