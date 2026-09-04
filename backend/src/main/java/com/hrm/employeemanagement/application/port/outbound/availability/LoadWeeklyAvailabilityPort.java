package com.hrm.employeemanagement.application.port.outbound.availability;

import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;

import java.util.Optional;

public interface LoadWeeklyAvailabilityPort {
    Optional<WeeklyAvailability> findByEmployeeIdAndYearWeek(Long employeeId, YearWeek yearWeek);
}
