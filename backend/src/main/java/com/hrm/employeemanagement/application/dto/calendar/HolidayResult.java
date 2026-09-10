package com.hrm.employeemanagement.application.dto.calendar;

import java.time.LocalDate;

public record HolidayResult(
        Long id,
        LocalDate holidayDate,
        String name,
        int workingHoursDeducted
) {}
