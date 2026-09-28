package com.hrm.employeemanagement.domain.task.dependency;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;

public class TaskDependency {
    private Long id;
    private final ProjectId projectId;
    private final TaskId predecessorId;
    private final TaskId successorId;
    private final TaskDependencyType dependencyType;
    private final Integer lagDays;
    private final UserId createdBy;
    private final LocalDateTime createdAt;

    public TaskDependency(
            Long id,
            ProjectId projectId,
            TaskId predecessorId,
            TaskId successorId,
            TaskDependencyType dependencyType,
            Integer lagDays,
            UserId createdBy,
            LocalDateTime createdAt) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId không được null");
        this.predecessorId = Objects.requireNonNull(predecessorId, "Predecessor TaskId không được null");
        this.successorId = Objects.requireNonNull(successorId, "Successor TaskId không được null");
        
        if (predecessorId.equals(successorId)) {
            throw new InvalidTaskDataException("Công việc không thể tự phụ thuộc vào chính mình");
        }

        if (lagDays != null && lagDays < 0) {
            throw new InvalidTaskDataException("Số ngày chờ không được nhỏ hơn 0");
        }

        this.id = id;
        this.dependencyType = dependencyType != null ? dependencyType : TaskDependencyType.FINISH_TO_START;
        this.lagDays = lagDays != null ? lagDays : 0;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static TaskDependency createNew(
            ProjectId projectId,
            TaskId predecessorId,
            TaskId successorId,
            TaskDependencyType dependencyType,
            Integer lagDays,
            UserId createdBy) {
        return new TaskDependency(
                null,
                projectId,
                predecessorId,
                successorId,
                dependencyType,
                lagDays,
                createdBy,
                LocalDateTime.now());
    }

    // Getters
    public Long getId() {
        return id;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public Long getProjectIdValue() {
        return projectId != null ? projectId.value() : null;
    }

    public TaskId getPredecessorId() {
        return predecessorId;
    }

    public Long getPredecessorIdValue() {
        return predecessorId != null ? predecessorId.value() : null;
    }

    public TaskId getSuccessorId() {
        return successorId;
    }

    public Long getSuccessorIdValue() {
        return successorId != null ? successorId.value() : null;
    }

    public TaskDependencyType getDependencyType() {
        return dependencyType;
    }

    public Integer getLagDays() {
        return lagDays;
    }

    public UserId getCreatedBy() {
        return createdBy;
    }

    public Long getCreatedByValue() {
        return createdBy != null ? createdBy.value() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDependency that = (TaskDependency) o;
        return Objects.equals(projectId, that.projectId)
                && Objects.equals(predecessorId, that.predecessorId)
                && Objects.equals(successorId, that.successorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, predecessorId, successorId);
    }
}
