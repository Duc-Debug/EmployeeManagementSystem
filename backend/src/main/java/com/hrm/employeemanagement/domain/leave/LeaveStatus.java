package com.hrm.employeemanagement.domain.leave;

public enum LeaveStatus {
    PENDING,          // Chờ quản lý nguồn lực phê duyệt (TC-01)
    APPROVED,         // Đã phê duyệt (Tính vào trừ giờ theo QTN-10)
    REJECTED,         // Bị từ chối
    CANCEL_REQUESTED, // Chờ duyệt hủy (nhân viên gửi yêu cầu hủy đơn đã duyệt)
    CANCELLED         // Đã hủy (QTN-22)
}
