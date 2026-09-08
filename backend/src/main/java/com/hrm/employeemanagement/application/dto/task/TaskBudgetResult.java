package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskBudgetBurnStatus;

/**
 * Kết quả trả về sau khi thiết lập ngân sách giờ công cho công việc.
 */
public record TaskBudgetResult(
        Long taskId,
        String taskCode,
        String name,
        BigDecimal budgetHours,
        BigDecimal actualHours,
        BigDecimal burnedPercentage,
        TaskBudgetBurnStatus burnStatus,
        BigDecimal remainingHours,
        BigDecimal overBudgetHours,
        boolean isOverBudget
) {
    public TaskBudgetResult(
            Long taskId,
            String taskCode,
            String name,
            BigDecimal budgetHours,
            BigDecimal actualHours,
            BigDecimal burnedPercentage,
            TaskBudgetBurnStatus burnStatus,
            BigDecimal remainingHours,
            boolean isOverBudget
    ) {
        this(
                taskId,
                taskCode,
                name,
                budgetHours,
                actualHours,
                burnedPercentage,
                burnStatus,
                remainingHours,
                BigDecimal.ZERO,
                isOverBudget
        );
    }

    public static TaskBudgetResult from(Task task) {
        return new TaskBudgetResult(
                task.getIdValue(),
                task.getTaskCode(),
                task.getName(),
                task.getBudgetHours(),
                task.getActualHours(),
                task.calculateBurnedPercentage(),
                task.getBudgetBurnStatus(),
                task.getRemainingBudgetHours(),
                task.getOverBudgetHours(),
                task.isOverBudget()
        );
    }
}
