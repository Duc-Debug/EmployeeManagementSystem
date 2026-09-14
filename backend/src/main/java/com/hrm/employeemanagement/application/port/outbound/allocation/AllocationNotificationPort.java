package com.hrm.employeemanagement.application.port.outbound.allocation;

/**
 * Outbound port gửi thông báo khi phân bổ thay đổi (QTN-15, NCL-07-CN-003, BR-02).
 * Gửi thông báo tới cả Quản lý dự án (PM) và Nhân viên chuyên môn bị ảnh hưởng.
 */
public interface AllocationNotificationPort {

    /**
     * Gửi thông báo thay đổi phân bổ cho Quản lý dự án và Nhân sự bị ảnh hưởng.
     */
    void notifyAllocationChanged(
            Long projectId,
            Long affectedEmployeeId,
            Long actorUserId,
            String title,
            String content
    );

    /**
     * Tương thích ngược: Gửi thông báo đơn giản cho PM.
     */
    String notifyAllocationAdjusted(Long projectId, Long managerId, String message);
}
