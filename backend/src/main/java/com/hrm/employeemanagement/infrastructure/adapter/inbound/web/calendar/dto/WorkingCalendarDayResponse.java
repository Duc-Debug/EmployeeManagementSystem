package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto;

import java.time.DayOfWeek;

public record WorkingCalendarDayResponse(
        DayOfWeek dayOfWeek,
        boolean isWorkingDay
) {}
