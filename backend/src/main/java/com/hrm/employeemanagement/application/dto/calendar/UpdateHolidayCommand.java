package com.hrm.employeemanagement.application.dto.calendar;

import java.time.LocalDate;

public record UpdateHolidayCommand(
        Long id,
        LocalDate holidayDate,
        String name,
        Integer workingHoursDeducted
) {}
