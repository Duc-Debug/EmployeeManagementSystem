package com.hrm.employeemanagement.application.dto.leave;

import java.math.BigDecimal;

/**
 * DTO kết quả thống kê số ngày phép còn lại (NCL-05-CN-005).
 */
public record LeaveBalanceResult(
        Long employeeId,
        int year,
        BigDecimal entitledDays,        // Số ngày phép được hưởng trong năm
        BigDecimal carriedOverDays,     // Số ngày phép năm ngoái chuyển sang
        BigDecimal totalAllocatedDays,  // Tổng quỹ phép (entitled + carriedOver)
        BigDecimal usedDays,            // Số ngày đã nghỉ (APPROVED)
        BigDecimal pendingDays,         // Số ngày đang chờ duyệt (PENDING)
        BigDecimal remainingDays        // Số ngày phép còn lại khả dụng
) {}
