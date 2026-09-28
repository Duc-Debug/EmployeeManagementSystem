package com.hrm.employeemanagement.application.port.outbound.availability;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LoadApprovedLeavesPort {
    /**
     * Lấy tổng số giờ nghỉ phép ĐÃ DUYỆT (APPROVED) của nhân viên trong khoảng thời gian.
     * Tuyệt đối không bao gồm các đơn PENDING hoặc REJECTED (QTN-10).
     */
    BigDecimal getTotalApprovedLeaveHoursBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * Nạp tổng số giờ nghỉ phép đã duyệt cho danh sách nhân sự theo các tuần mục tiêu (batch loading 1 SQL query).
     */
    java.util.Map<Long, java.util.Map<com.hrm.employeemanagement.domain.availability.YearWeek, BigDecimal>> loadApprovedLeaveHoursForEmployeesAndWeeks(
            java.util.List<Long> employeeIds,
            java.util.List<com.hrm.employeemanagement.domain.availability.YearWeek> targetWeeks
    );
}
