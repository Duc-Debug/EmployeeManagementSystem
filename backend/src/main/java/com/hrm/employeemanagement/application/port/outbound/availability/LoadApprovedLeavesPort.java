package com.hrm.employeemanagement.application.port.outbound.availability;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LoadApprovedLeavesPort {
    /**
     * Lấy tổng số giờ nghỉ phép ĐÃ DUYỆT (APPROVED) của nhân viên trong khoảng thời gian.
     * Tuyệt đối không bao gồm các đơn PENDING hoặc REJECTED (QTN-10).
     */
    BigDecimal getTotalApprovedLeaveHoursBetween(Long employeeId, LocalDate startDate, LocalDate endDate);
}
