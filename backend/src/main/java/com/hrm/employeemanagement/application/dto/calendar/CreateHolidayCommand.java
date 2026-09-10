package com.hrm.employeemanagement.application.dto.calendar;

import java.time.LocalDate;

public record CreateHolidayCommand(
        LocalDate holidayDate,
        String name,
        Integer workingHoursDeducted
) {}
