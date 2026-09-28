package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;

/**
 * Lệnh đặt ngân sách giờ công cho công việc trong dự án.
 */
public record SetTaskBudgetCommand(
        Long projectId,
        Long taskId,
        BigDecimal budgetHours
) {
}
