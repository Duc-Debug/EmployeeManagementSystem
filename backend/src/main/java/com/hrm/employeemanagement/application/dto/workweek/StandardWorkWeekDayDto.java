package com.hrm.employeemanagement.application.dto.workweek;

import java.math.BigDecimal;
import java.time.DayOfWeek;

public record StandardWorkWeekDayDto(
        DayOfWeek dayOfWeek,
        boolean isWorkingDay,
        BigDecimal workingHours
) {}

