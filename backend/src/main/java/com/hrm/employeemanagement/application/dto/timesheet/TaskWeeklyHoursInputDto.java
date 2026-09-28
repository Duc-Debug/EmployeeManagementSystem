package com.hrm.employeemanagement.application.dto.timesheet;

import java.util.List;

public record TaskWeeklyHoursInputDto(
    Long projectId,
    Long taskId,
    Boolean isBillable,
    String description,
    List<TaskDailyHourInputDto> dailyHours
) {}

