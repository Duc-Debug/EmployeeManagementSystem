package com.hrm.employeemanagement.domain.projecttemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.task.TaskType;

public class ProjectTemplateTask {
    private final ProjectTemplateTaskId id;
    private final ProjectTemplateId templateId;
    private final ProjectTemplateTaskId parentId;
    private final String name;
    private final String description;
    private final TaskType taskType;
    private final BigDecimal estimatedHours;
    private final Integer sortOrder;
    private final LocalDateTime createdAt;

    public ProjectTemplateTask(
            ProjectTemplateTaskId id,
            ProjectTemplateId templateId,
            ProjectTemplateTaskId parentId,
            String name,
            String description,
            TaskType taskType,
            BigDecimal estimatedHours,
            Integer sortOrder,
            LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "ProjectTemplateTaskId must not be null");
        this.templateId = Objects.requireNonNull(templateId, "ProjectTemplateId must not be null");
        this.parentId = parentId;
        this.name = Objects.requireNonNull(name, "Template task name must not be null");
        this.description = description;
        this.taskType = taskType != null ? taskType : TaskType.TASK;
        this.estimatedHours = estimatedHours != null ? estimatedHours : BigDecimal.ZERO;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.createdAt = createdAt;
    }

    public ProjectTemplateTaskId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public ProjectTemplateId getTemplateId() {
        return templateId;
    }

    public Long getTemplateIdValue() {
        return templateId != null ? templateId.value() : null;
    }

    public ProjectTemplateTaskId getParentId() {
        return parentId;
    }

    public Long getParentIdValue() {
        return parentId != null ? parentId.value() : null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
