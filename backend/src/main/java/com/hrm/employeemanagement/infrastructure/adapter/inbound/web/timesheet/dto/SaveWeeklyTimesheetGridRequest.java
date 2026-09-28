package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SaveWeeklyTimesheetGridRequest(
        LocalDate dateInWeek,

        @NotNull(message = "Danh sách công việc không được để trống.")
        @Valid
        List<TaskWeeklyHoursRequest> taskEntries
) {
    public record TaskWeeklyHoursRequest(
            @NotNull(message = "Dự án không được để trống.")
            Long projectId,

            @NotNull(message = "Công việc không được để trống.")
            Long taskId,

            Boolean isBillable,

            String description,

            List<TaskDailyHourRequest> dailyHours
    ) {}

    public record TaskDailyHourRequest(
            @NotNull(message = "Ngày làm việc không được để trống.")
            LocalDate workDate,

            BigDecimal hours
    ) {}
}

