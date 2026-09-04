package com.hrm.employeemanagement.application.port.outbound.availability;

import com.hrm.employeemanagement.domain.availability.Holiday;

import java.time.LocalDate;
import java.util.List;

public interface LoadHolidaysPort {
    List<LocalDate> getHolidayDatesBetween(LocalDate startDate, LocalDate endDate);
    List<Holiday> getHolidaysBetween(LocalDate startDate, LocalDate endDate);
}
