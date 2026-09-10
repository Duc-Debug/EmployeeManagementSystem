package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto;

import com.hrm.employeemanagement.application.dto.calendar.CompanyWorkingCalendarResult;

import java.util.List;

public record WorkingCalendarResponse(
        List<WorkingCalendarDayResponse> days
) {
    public static WorkingCalendarResponse from(CompanyWorkingCalendarResult result) {
        return new WorkingCalendarResponse(
                result.days().stream()
                        .map(d -> new WorkingCalendarDayResponse(d.dayOfWeek(), d.isWorkingDay()))
                        .toList()
        );
    }
}
