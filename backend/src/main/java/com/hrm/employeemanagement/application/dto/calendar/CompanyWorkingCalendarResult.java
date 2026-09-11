package com.hrm.employeemanagement.application.dto.calendar;

import java.util.List;

public record CompanyWorkingCalendarResult(
        List<WorkingCalendarDayDto> days
) {}
