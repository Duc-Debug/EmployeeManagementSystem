package com.hrm.employeemanagement.domain.timesheet;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.user.UserId;

public class TimesheetAuditLog {
    private TimesheetAuditLogId id;
    private final TimesheetId timesheetId;
    private final TimesheetEntryId entryId;
    private final String action;
    private final UserId actorId;
    private final String note;
    private final LocalDateTime createdAt;

    public TimesheetAuditLog(
            TimesheetAuditLogId id,
            TimesheetId timesheetId,
            TimesheetEntryId entryId,
            String action,
            UserId actorId,
            String note,
            LocalDateTime createdAt) {
        this.id = id;
        this.timesheetId = Objects.requireNonNull(timesheetId, "timesheetId must not be null");
        this.entryId = entryId;
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        this.note = note;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static TimesheetAuditLog create(
            TimesheetId timesheetId,
            TimesheetEntryId entryId,
            String action,
            UserId actorId,
            String note) {
        return new TimesheetAuditLog(
                null,
                timesheetId,
                entryId,
                action,
                actorId,
                note,
                LocalDateTime.now()
        );
    }

    public TimesheetAuditLogId getId() { return id; }
    public Long getIdValue() { return id != null ? id.value() : null; }
    public TimesheetId getTimesheetId() { return timesheetId; }
    public Long getTimesheetIdValue() { return timesheetId != null ? timesheetId.value() : null; }
    public TimesheetEntryId getEntryId() { return entryId; }
    public Long getEntryIdValue() { return entryId != null ? entryId.value() : null; }
    public String getAction() { return action; }
    public UserId getActorId() { return actorId; }
    public Long getActorIdValue() { return actorId != null ? actorId.value() : null; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
