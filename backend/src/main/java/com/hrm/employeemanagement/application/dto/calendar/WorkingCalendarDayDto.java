package com.hrm.employeemanagement.application.dto.calendar;

import java.time.DayOfWeek;

public record WorkingCalendarDayDto(
        DayOfWeek dayOfWeek,
        boolean isWorkingDay
) {}
