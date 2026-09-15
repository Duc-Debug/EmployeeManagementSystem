package com.hrm.employeemanagement.infrastructure.persistence.timesheet;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "timesheet_audit_logs")
public class TimesheetAuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timesheet_id", nullable = false)
    private Long timesheetId;

    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTimesheetId() { return timesheetId; }
    public void setTimesheetId(Long timesheetId) { this.timesheetId = timesheetId; }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
