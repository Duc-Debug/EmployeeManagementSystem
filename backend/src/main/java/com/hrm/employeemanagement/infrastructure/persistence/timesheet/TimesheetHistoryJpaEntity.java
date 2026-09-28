package com.hrm.employeemanagement.infrastructure.persistence.timesheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "timesheet_histories")
public class TimesheetHistoryJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timesheet_id", nullable = false)
    private Long timesheetId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "action_by")
    private Long actionBy;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors, Getters, Setters
    public TimesheetHistoryJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTimesheetId() { return timesheetId; }
    public void setTimesheetId(Long timesheetId) { this.timesheetId = timesheetId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getActionBy() { return actionBy; }
    public void setActionBy(Long actionBy) { this.actionBy = actionBy; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
