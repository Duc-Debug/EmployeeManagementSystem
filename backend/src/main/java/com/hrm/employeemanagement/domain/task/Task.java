package com.hrm.employeemanagement.domain.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private BigDecimal budgetHours;
    private TaskStatus status;
    private Integer sortOrder;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate actualEndDate;
    private Integer slackDays;
    private UserId createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    /**
     * Constructor đầy đủ tất cả tham số (kết hợp cả 2 nhánh)
     */
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
            BigDecimal budgetHours,
            TaskStatus status,
            Integer sortOrder,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate startDate,
            LocalDate dueDate,
            LocalDate actualEndDate,
            Integer slackDays,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        validateProjectId(projectId);
        validateName(name);
        validateEstimatedHours(estimatedHours);
        validateActualHours(actualHours);
        validateBudgetHours(budgetHours);
        validateTaskTypeAndAssignee(taskType, assigneeId);
        validateSortOrder(sortOrder);
        
        LocalDate effectivePlannedStart = plannedStartDate != null ? plannedStartDate : startDate;
        LocalDate effectivePlannedEnd = plannedEndDate != null ? plannedEndDate : dueDate;
        validatePlannedDates(effectivePlannedStart, effectivePlannedEnd);

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
        this.budgetHours = budgetHours != null ? budgetHours : BigDecimal.ZERO;
        this.status = status != null ? status : TaskStatus.TODO;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.plannedStartDate = effectivePlannedStart;
        this.plannedEndDate = effectivePlannedEnd;
        this.startDate = startDate != null ? startDate : plannedStartDate;
        this.dueDate = dueDate != null ? dueDate : plannedEndDate;
        this.actualEndDate = actualEndDate;
        this.slackDays = slackDays != null ? slackDays : 0;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /**
     * Constructor tương thích nhánh feat/task-assignment (plannedStartDate, plannedEndDate)
     */
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
            BigDecimal budgetHours,
            TaskStatus status,
            Integer sortOrder,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                estimatedHours,
                actualHours,
                budgetHours,
                status,
                sortOrder,
                plannedStartDate,
                plannedEndDate,
                plannedStartDate,
                plannedEndDate,
                null,
                0,
                createdBy,
                createdAt,
                updatedAt,
                version
        );
    }

    /**
     * Constructor tương thích nhánh develop (startDate, dueDate, actualEndDate, slackDays)
     */
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
            BigDecimal budgetHours,
            TaskStatus status,
            Integer sortOrder,
            LocalDate startDate,
            LocalDate dueDate,
            LocalDate actualEndDate,
            Integer slackDays,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                estimatedHours,
                actualHours,
                budgetHours,
                status,
                sortOrder,
                startDate,
                dueDate,
                startDate,
                dueDate,
                actualEndDate,
                slackDays,
                createdBy,
                createdAt,
                updatedAt,
                version
        );
    }

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
            BigDecimal budgetHours,
            TaskStatus status,
            Integer sortOrder,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                estimatedHours,
                actualHours,
                budgetHours,
                status,
                sortOrder,
                null,
                null,
                null,
                null,
                null,
                0,
                createdBy,
                createdAt,
                updatedAt,
                version);
    }

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
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                estimatedHours,
                actualHours,
                BigDecimal.ZERO,
                status,
                sortOrder,
                createdBy,
                createdAt,
                updatedAt,
                version);
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
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description.trim().isEmpty() ? null : description.trim();
        }
        if (estimatedHours != null) {
            validateEstimatedHours(estimatedHours);
            this.estimatedHours = estimatedHours;
        }
        if (sortOrder != null) {
            validateSortOrder(sortOrder);
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

    public void setBudgetHours(BigDecimal budgetHours) {
        if (budgetHours == null || budgetHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTaskDataException("Ngân sách giờ công phải lớn hơn 0");
        }
        this.budgetHours = budgetHours;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal calculateBurnedPercentage() {
        if (this.budgetHours == null || this.budgetHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal actual = this.actualHours != null ? this.actualHours : BigDecimal.ZERO;
        return actual.multiply(BigDecimal.valueOf(100)).divide(this.budgetHours, 2, RoundingMode.HALF_UP);
    }

    public boolean isOverBudget() {
        return TaskBudgetPolicy.isOverBudget(this.budgetHours, this.actualHours);
    }

    public BigDecimal getRemainingBudgetHours() {
        if (this.budgetHours == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal actual = this.actualHours != null ? this.actualHours : BigDecimal.ZERO;
        BigDecimal remaining = this.budgetHours.subtract(actual);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    public BigDecimal getOverBudgetHours() {
        if (this.budgetHours == null || this.actualHours == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal over = this.actualHours.subtract(this.budgetHours);
        return over.compareTo(BigDecimal.ZERO) > 0 ? over : BigDecimal.ZERO;
    }

    public TaskBudgetBurnStatus getBudgetBurnStatus() {
        return TaskBudgetPolicy.determineBurnStatus(this.budgetHours, calculateBurnedPercentage());
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

    private void validateBudgetHours(BigDecimal hours) {
        if (hours != null && hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTaskDataException("Ngân sách giờ công không được nhỏ hơn 0");
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

    public BigDecimal getBudgetHours() {
        return budgetHours;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public Integer getSlackDays() {
        return slackDays != null ? slackDays : 0;
    }

    public void updateActualEndDate(LocalDate actualEndDate) {
        if (actualEndDate != null && this.startDate != null && actualEndDate.isBefore(this.startDate)) {
            throw new InvalidTaskDataException("Ngày kết thúc thực tế (" + actualEndDate + ") không được nhỏ hơn ngày bắt đầu (" + this.startDate + ")");
        }
        this.actualEndDate = actualEndDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void setScheduleDates(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new InvalidTaskDataException("Ngày hoàn thành kế hoạch không được trước ngày bắt đầu");
        }
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.plannedStartDate = startDate;
        this.plannedEndDate = dueDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void setSlackDays(Integer slackDays) {
        if (slackDays != null && slackDays < 0) {
            throw new InvalidTaskDataException("Thời gian dự phòng (slack days) không được nhỏ hơn 0");
        }
        this.slackDays = slackDays != null ? slackDays : 0;
        this.updatedAt = LocalDateTime.now();
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

    private void validatePlannedDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidTaskDataException("Ngày kết thúc mong muốn không được trước ngày bắt đầu mong muốn");
        }
    }

    public void updatePlannedDates(LocalDate plannedStartDate, LocalDate plannedEndDate) {
        validatePlannedDates(plannedStartDate, plannedEndDate);
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.startDate = plannedStartDate;
        this.dueDate = plannedEndDate;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public Long getVersion() {
        return version;
    }
}
