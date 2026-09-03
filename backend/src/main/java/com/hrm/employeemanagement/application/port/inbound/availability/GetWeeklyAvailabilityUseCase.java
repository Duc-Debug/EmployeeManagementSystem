package com.hrm.employeemanagement.application.port.inbound.availability;

import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;

public interface GetWeeklyAvailabilityUseCase {
    WeeklyAvailabilityResult getAvailability(Long employeeId, Integer year, Integer weekNumber);
}
