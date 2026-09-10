package com.hrm.employeemanagement.application.dto.project;

import java.math.BigDecimal;
/**
 * DTO tóm tắt bảng chấm công chờ duyệt 
 */
public record PendingTimesheetSummary(
        Long timesheetId,
        Long employeeId,
        String employeeName,
        String period,
        BigDecimal hours) {
}
