package com.hrm.employeemanagement.application.port.outbound.calendar;

import java.time.LocalDate;

public interface HolidayCommandPort {
    HolidayRecord create(LocalDate date, String name, int workingHoursDeducted);
    HolidayRecord update(Long id, LocalDate date, String name, int workingHoursDeducted);
    void deleteById(Long id);
}
