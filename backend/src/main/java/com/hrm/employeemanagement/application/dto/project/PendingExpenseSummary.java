package com.hrm.employeemanagement.application.dto.project;

import java.math.BigDecimal;
/**
 * DTO tóm tắt chi phí chờ duyệt
 */
public record PendingExpenseSummary(
        Long expenseId,
        String title,
        BigDecimal amount,
        String status) {
}
