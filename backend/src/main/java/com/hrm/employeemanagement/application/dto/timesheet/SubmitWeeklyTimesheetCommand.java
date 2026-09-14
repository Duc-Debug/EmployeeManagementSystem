package com.hrm.employeemanagement.application.dto.timesheet;

import java.time.LocalDate;
/**
 * Dto nộp bảng chấm công theo tuần
 */
public record SubmitWeeklyTimesheetCommand(
        LocalDate dateInWeek,
        Long timesheetId
) {
    public SubmitWeeklyTimesheetCommand(LocalDate dateInWeek) {
        this(dateInWeek, null);
    }
}