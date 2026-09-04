package com.hrm.employeemanagement.application.port.outbound.availability;

import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;

public interface SaveWeeklyAvailabilityPort {
    WeeklyAvailability save(WeeklyAvailability availability);
}
