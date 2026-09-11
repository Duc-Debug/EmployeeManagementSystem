package com.hrm.employeemanagement.domain.task;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.user.UserId;

public class TaskAssignment {

    private Long id;
    private TaskId taskId;
    private EmployeeId employeeId;
    private LocalDateTime assignedAt;
    private UserId assignedBy;
    private boolean primary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public TaskAssignment(
            Long id,
            TaskId taskId,
            EmployeeId employeeId,
            LocalDateTime assignedAt,
            UserId assignedBy,
            boolean primary,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.id = id;
        this.taskId = Objects.requireNonNull(taskId, "TaskId không được null");
        this.employeeId = Objects.requireNonNull(employeeId, "EmployeeId không được null");
        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.assignedBy = assignedBy;
        this.primary = primary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static TaskAssignment create(TaskId taskId, EmployeeId employeeId, UserId assignedBy, boolean isPrimary) {
        return new TaskAssignment(
                null,
                taskId,
                employeeId,
                LocalDateTime.now(),
                assignedBy,
                isPrimary,
                LocalDateTime.now(),
                null,
                null
        );
    }

    public Long getId() {
        return id;
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public EmployeeId getEmployeeId() {
        return employeeId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public UserId getAssignedBy() {
        return assignedBy;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}

