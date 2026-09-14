package com.hrm.employeemanagement.application.port.outbound.allocation;

/**
 * Outbound port gửi thông báo cho Quản lý dự án (QLDA) liên quan khi phân bổ thay đổi (QTN-15, K6).
 */
public interface AllocationNotificationPort {

    String notifyAllocationAdjusted(Long projectId, Long managerId, String message);
}
