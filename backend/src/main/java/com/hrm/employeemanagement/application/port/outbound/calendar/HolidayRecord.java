package com.hrm.employeemanagement.application.port.outbound.calendar;

import java.time.LocalDate;

public record HolidayRecord(
        Long id,
        LocalDate date,
        String name,
        int workingHoursDeducted
) {}
