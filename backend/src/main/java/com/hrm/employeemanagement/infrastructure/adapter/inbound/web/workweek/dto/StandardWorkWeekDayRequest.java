package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;

public record StandardWorkWeekDayRequest(
        @NotNull(message = "Thứ trong tuần không được null")
        DayOfWeek dayOfWeek,

        @NotNull(message = "isWorkingDay không được null")
        Boolean isWorkingDay,

        BigDecimal workingHours
) {}

