package com.hrm.employeemanagement.application.dto.timesheet;

import java.time.LocalDate;
import java.util.List;

public record SaveWeeklyTimesheetGridCommand(
    LocalDate dateInWeek,
    List<TaskWeeklyHoursInputDto> taskEntries
) {}

