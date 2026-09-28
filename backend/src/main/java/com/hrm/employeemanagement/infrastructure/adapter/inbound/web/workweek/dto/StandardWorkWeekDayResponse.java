package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;

public record StandardWorkWeekDayResponse(
        DayOfWeek dayOfWeek,
        boolean isWorkingDay,
        BigDecimal workingHours
) {}

