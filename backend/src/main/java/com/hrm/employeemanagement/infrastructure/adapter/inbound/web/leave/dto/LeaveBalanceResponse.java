package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
        Long employeeId,
        int year,
        BigDecimal entitledDays,        // Số ngày phép năm được hưởng
        BigDecimal carriedOverDays,     // Số ngày phép năm ngoái chuyển sang
        BigDecimal totalAllocatedDays,  // Tổng quỹ phép năm
        BigDecimal usedDays,            // Số ngày đã nghỉ (APPROVED)
        BigDecimal pendingDays,         // Số ngày đang chờ duyệt (PENDING)
        BigDecimal remainingDays        // Số ngày phép còn lại khả dụng
) {
    public static LeaveBalanceResponse fromResult(LeaveBalanceResult result) {
        if (result == null) {
            return null;
        }
        return new LeaveBalanceResponse(
                result.employeeId(),
                result.year(),
                result.entitledDays(),
                result.carriedOverDays(),
                result.totalAllocatedDays(),
                result.usedDays(),
                result.pendingDays(),
                result.remainingDays()
        );
    }
}
