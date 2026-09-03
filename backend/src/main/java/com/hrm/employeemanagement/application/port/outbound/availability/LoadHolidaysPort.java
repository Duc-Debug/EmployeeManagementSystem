package com.hrm.employeemanagement.application.port.outbound.availability;

import java.time.LocalDate;
import java.util.List;

public interface LoadHolidaysPort {
    List<LocalDate> getHolidayDatesBetween(LocalDate startDate, LocalDate endDate);
}
