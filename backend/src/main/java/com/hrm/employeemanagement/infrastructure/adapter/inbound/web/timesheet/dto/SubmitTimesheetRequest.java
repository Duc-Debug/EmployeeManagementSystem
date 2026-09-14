package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto;

import java.time.LocalDate;

public record SubmitTimesheetRequest(
        LocalDate dateInWeek,
        Long timesheetId
) {}