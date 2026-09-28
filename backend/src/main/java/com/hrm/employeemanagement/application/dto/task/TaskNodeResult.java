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
        if (budgetHours == null) {
            budgetHours = BigDecimal.ZERO;
        }
        if (burnedPercentage == null) {
            burnedPercentage = BigDecimal.ZERO;
        }
        if (burnStatus == null) {
            burnStatus = TaskBudgetBurnStatus.NOT_SET;
        }
        if (isOverBudget == null) {
            isOverBudget = false;
        }
        if (slackDays == null) {
            slackDays = 0;
        }
    }

    /**
     * Constructor rút gọn không truyền startDate/dueDate/actualEndDate (giữ giá trị null độc lập với planned dates).
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
                budgetHours,
                burnedPercentage,
                burnStatus,
                isOverBudget,
                status,
                sortOrder,
                plannedStartDate,
                plannedEndDate,
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
                task.assigneeId() != null ? List.of(task.assigneeId()) : List.of(),
                task.estimatedHours(),
                task.actualHours(),
                task.budgetHours(),
                BigDecimal.ZERO,
                TaskBudgetBurnStatus.NOT_SET,
                false,
                task.status(),
                task.sortOrder(),
                null,
                null,
                null,
                null,
                null,
                0,
                task.createdBy(),
                task.createdAt(),
                task.updatedAt(),
                task.version(),
                new ArrayList<>()
        );
    }
}