package com.hrm.employeemanagement.domain.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.UserId;

public class Task {
    private TaskId id;
    private ProjectId projectId;
    private TaskId parentId;
    private String taskCode;
    private String name;
    private String description;
    private TaskType taskType;
    private EmployeeId assigneeId;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private TaskStatus status;
    private Integer sortOrder;
    private UserId createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public Task(
            TaskId id,
            ProjectId projectId,
            TaskId parentId,
            String taskCode,
            String name,
            String description,
            TaskType taskType,
            EmployeeId assigneeId,
            BigDecimal estimatedHours,
            BigDecimal actualHours,
            TaskStatus status,
            Integer sortOrder,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        validateProjectId(projectId);
        validateName(name);
        validateEstimatedHours(estimatedHours);
        validateActualHours(actualHours);
        validateTaskTypeAndAssignee(taskType, assigneeId);
        validateSortOrder(sortOrder);
        this.id = id;
        this.projectId = projectId;
        this.parentId = parentId;
        this.taskCode = taskCode != null ? taskCode.trim() : null;
        this.name = name.trim();
        this.description = description != null ? description.trim() : null;
        this.taskType = taskType != null ? taskType : TaskType.TASK;
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

    public static Task createNew(
            ProjectId projectId,
            TaskId parentId,
            String taskCode,
            String name,
            String description,
            TaskType taskType,
            EmployeeId assigneeId,
            BigDecimal estimatedHours,
            Integer sortOrder,
            UserId createdBy) {
        return new Task(
                null,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                estimatedHours,
                BigDecimal.ZERO,
                TaskStatus.TODO,
                sortOrder,
                createdBy,
                LocalDateTime.now(),
                null,
                null);
    }

    public void updateDetails(String name, String description, BigDecimal estimatedHours, Integer sortOrder) {
        validateName(name);
        validateEstimatedHours(estimatedHours);
        validateSortOrder(sortOrder);
        this.name = name.trim();
        if (description != null) {
            this.description = description.trim().isEmpty() ? null : description.trim();
        }
        if (estimatedHours != null) {
            this.estimatedHours = estimatedHours;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Business behaviors
    public void changeParent(TaskId newParentId) {
        if (this.id != null && Objects.equals(this.id, newParentId)) {
            throw new InvalidTaskDataException("Công việc không thể chọn chính mình làm công việc cha");
        }
        this.parentId = newParentId;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignTo(EmployeeId newAssigneeId) {
        if (this.taskType == TaskType.CATEGORY && newAssigneeId != null) {
            throw new InvalidTaskDataException("Hạng mục gom nhóm không được gán người thực hiện trực tiếp");
        }
        this.assigneeId = newAssigneeId;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidTaskDataException("Trạng thái công việc không được để trống");
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCategory() {
        return this.taskType == TaskType.CATEGORY;
    }

    public boolean isTask() {
        return this.taskType == TaskType.TASK;
    }

    // Validations
    private void validateProjectId(ProjectId projectId) {
        if (projectId == null || projectId.value() == null) {
            throw new InvalidTaskDataException("Mã dự án (projectId) không được để trống");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidTaskDataException("Tên hạng mục / công việc không được để trống");
        }
        if (name.trim().length() > 255) {
            throw new InvalidTaskDataException("Tên hạng mục / công việc không được vượt quá 255 ký tự");
        }
    }

    private void validateEstimatedHours(BigDecimal hours) {
        if (hours != null && hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTaskDataException("Thời gian dự kiến không được nhỏ hơn 0");
        }
    }

    private void validateActualHours(BigDecimal hours) {
        if (hours != null && hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTaskDataException("Thời gian thực tế không được nhỏ hơn 0");
        }
    }

    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder < 0) {
            throw new InvalidTaskDataException("Thứ tự sắp xếp không được nhỏ hơn 0");
        }
    }

    private void validateTaskTypeAndAssignee(TaskType type, EmployeeId assignee) {
        if (type == TaskType.CATEGORY && assignee != null) {
            throw new InvalidTaskDataException("Hạng mục gom nhóm không được gán người thực hiện trực tiếp");
        }
    }

    // Getters
    public TaskId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public Long getProjectIdValue() {
        return projectId != null ? projectId.value() : null;
    }

    public TaskId getParentId() {
        return parentId;
    }

    public Long getParentIdValue() {
        return parentId != null ? parentId.value() : null;
    }

    public String getTaskCode() {
        return taskCode;
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

    public EmployeeId getAssigneeId() {
        return assigneeId;
    }

    public Long getAssigneeIdValue() {
        return assigneeId != null ? assigneeId.value() : null;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}