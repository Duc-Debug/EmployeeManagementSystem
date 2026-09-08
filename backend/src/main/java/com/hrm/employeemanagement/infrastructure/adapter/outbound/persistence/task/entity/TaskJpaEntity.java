package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tasks")
public class TaskJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "task_code", length = 50)
    private String taskCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "estimated_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedHours;

    @Column(name = "actual_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public TaskJpaEntity() {
    }

    public TaskJpaEntity(
            Long id,
            Long projectId,
            Long parentId,
            String taskCode,
            String name,
            String description,
            TaskType taskType,
            Long assigneeId,
            BigDecimal estimatedHours,
            BigDecimal actualHours,
            TaskStatus status,
            Integer sortOrder,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.id = id;
        this.projectId = projectId;
        this.parentId = parentId;
        this.taskCode = taskCode;
        this.name = name;
        this.description = description;
        this.taskType = taskType;
        this.assigneeId = assigneeId;
        this.estimatedHours = estimatedHours != null ? estimatedHours : BigDecimal.ZERO;
        this.actualHours = actualHours != null ? actualHours : BigDecimal.ZERO;
        this.status = status != null ? status : TaskStatus.TODO;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TaskStatus.TODO;
        }
        if (taskType == null) {
            taskType = TaskType.TASK;
        }
        if (estimatedHours == null) {
            estimatedHours = BigDecimal.ZERO;
        }
        if (actualHours == null) {
            actualHours = BigDecimal.ZERO;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(BigDecimal estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public void setActualHours(BigDecimal actualHours) {
        this.actualHours = actualHours;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
