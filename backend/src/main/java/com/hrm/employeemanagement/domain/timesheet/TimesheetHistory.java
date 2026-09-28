package com.hrm.employeemanagement.domain.timesheet;

import com.hrm.employeemanagement.domain.user.UserId;

import java.time.LocalDateTime;

public class TimesheetHistory {
    private final Long id;
    private final TimesheetId timesheetId;
    private final String action;
    private final UserId actionBy;
    private final String content;
    private final LocalDateTime createdAt;

    public TimesheetHistory(Long id, TimesheetId timesheetId, String action, UserId actionBy, String content, LocalDateTime createdAt) {
        this.id = id;
        this.timesheetId = timesheetId;
        this.action = action;
        this.actionBy = actionBy;
        this.content = content;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static TimesheetHistory create(TimesheetId timesheetId, String action, UserId actionBy, String content) {
        return new TimesheetHistory(null, timesheetId, action, actionBy, content, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public TimesheetId getTimesheetId() {
        return timesheetId;
    }

    public String getAction() {
        return action;
    }

    public UserId getActionBy() {
        return actionBy;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
