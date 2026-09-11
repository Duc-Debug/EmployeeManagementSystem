package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hrm.employeemanagement.domain.task.TaskBudgetBurnStatus;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;

/**
 * Đại diện cho cấu trúc cây WBS (Tree node) có danh sách các node con children
 * lồng nhau, kèm thông số ngân sách giờ công từ Domain.
 */
public record TaskNodeResult(
        Long id,
        Long projectId,
        Long parentId,
        String taskCode,
        String name,
        String description,
        TaskType taskType,
        Long assigneeId,
        List<Long> assigneeIds,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        BigDecimal budgetHours,
        BigDecimal burnedPercentage,
        TaskBudgetBurnStatus burnStatus,
        Boolean isOverBudget,
        TaskStatus status,
        Integer sortOrder,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate startDate,
        LocalDate dueDate,
        LocalDate actualEndDate,
        Integer slackDays,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version,
        List<TaskNodeResult> children) {

    public TaskNodeResult {
        if (children == null) {
            children = new ArrayList<>();
        }
        if (assigneeIds == null) {
            assigneeIds = assigneeId != null ? List.of(assigneeId) : List.of();
        }
        if (startDate == null && plannedStartDate != null) {
            startDate = plannedStartDate;
        }
        if (plannedStartDate == null && startDate != null) {
            plannedStartDate = startDate;
        }
        if (dueDate == null && plannedEndDate != null) {
            dueDate = plannedEndDate;
        }
        if (plannedEndDate == null && dueDate != null) {
            plannedEndDate = dueDate;
        }
    }

    /**
     * Constructor hỗ trợ plannedStartDate, plannedEndDate (từ nhánh feat/task-assignment)
     */
    public TaskNodeResult(
            Long id,
            Long projectId,
            Long parentId,
            String taskCode,
            String name,
            String description,
            TaskType taskType,
            Long assigneeId,
            List<Long> assigneeIds,
            BigDecimal estimatedHours,
            BigDecimal actualHours,
            BigDecimal budgetHours,
            BigDecimal burnedPercentage,
            TaskBudgetBurnStatus burnStatus,
            Boolean isOverBudget,
            TaskStatus status,
            Integer sortOrder,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version,
            List<TaskNodeResult> children
    ) {
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                assigneeIds,
                estimatedHours,
                actualHours,
                budgetHours != null ? budgetHours : BigDecimal.ZERO,
                burnedPercentage != null ? burnedPercentage : BigDecimal.ZERO,
                burnStatus != null ? burnStatus : TaskBudgetBurnStatus.NOT_SET,
                isOverBudget != null ? isOverBudget : false,
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
                version,
                children
        );
    }

    /**
     * Constructor hỗ trợ startDate, dueDate, actualEndDate, slackDays (từ nhánh develop)
     */
    public TaskNodeResult(
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
            BigDecimal budgetHours,
            BigDecimal burnedPercentage,
            TaskBudgetBurnStatus burnStatus,
            Boolean isOverBudget,
            TaskStatus status,
            Integer sortOrder,
            LocalDate startDate,
            LocalDate dueDate,
            LocalDate actualEndDate,
            Integer slackDays,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version,
            List<TaskNodeResult> children
    ) {
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                assigneeId != null ? List.of(assigneeId) : List.of(),
                estimatedHours,
                actualHours,
                budgetHours != null ? budgetHours : BigDecimal.ZERO,
                burnedPercentage != null ? burnedPercentage : BigDecimal.ZERO,
                burnStatus != null ? burnStatus : TaskBudgetBurnStatus.NOT_SET,
                isOverBudget != null ? isOverBudget : false,
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
                version,
                children
        );
    }

    /**
     * Constructor 21 tham số (tương thích backward)
     */
    public TaskNodeResult(
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
            BigDecimal budgetHours,
            BigDecimal burnedPercentage,
            TaskBudgetBurnStatus burnStatus,
            Boolean isOverBudget,
            TaskStatus status,
            Integer sortOrder,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version,
            List<TaskNodeResult> children
    ) {
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                assigneeId != null ? List.of(assigneeId) : List.of(),
                estimatedHours,
                actualHours,
                budgetHours != null ? budgetHours : BigDecimal.ZERO,
                burnedPercentage != null ? burnedPercentage : BigDecimal.ZERO,
                burnStatus != null ? burnStatus : TaskBudgetBurnStatus.NOT_SET,
                isOverBudget != null ? isOverBudget : false,
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
                version,
                children
        );
    }

    /**
     * Constructor 18 tham số (có budgetHours)
     */
    public TaskNodeResult(
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
            BigDecimal budgetHours,
            TaskStatus status,
            Integer sortOrder,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version,
            List<TaskNodeResult> children
    ) {
        this(
                id,
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType,
                assigneeId,
                assigneeId != null ? List.of(assigneeId) : List.of(),
                estimatedHours,
                actualHours,
                budgetHours != null ? budgetHours : BigDecimal.ZERO,
                BigDecimal.ZERO,
                TaskBudgetBurnStatus.NOT_SET,
                false,
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
                version,
                children
        );
    }

    /**
     * Constructor 17 tham số (không có budgetHours)
     */
    public TaskNodeResult(
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
            Long version,
            List<TaskNodeResult> children
    ) {
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
                version,
                children
        );
    }

    public static TaskNodeResult from(TaskResult task) {
        return new TaskNodeResult(
                task.id(),
                task.projectId(),
                task.parentId(),
                task.taskCode(),
                task.name(),
                task.description(),
                task.taskType(),
                task.assigneeId(),
                task.estimatedHours(),
                task.actualHours(),
                task.budgetHours(),
                task.status(),
                task.sortOrder(),
                task.createdBy(),
                task.createdAt(),
                task.updatedAt(),
                task.version(),
                new ArrayList<>()
        );
    }
}