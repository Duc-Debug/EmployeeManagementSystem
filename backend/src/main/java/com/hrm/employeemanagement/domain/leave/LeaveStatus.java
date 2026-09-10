package com.hrm.employeemanagement.domain.leave;

public enum LeaveStatus {
    PENDING,   // Chờ quản lý nguồn lực phê duyệt (TC-01)
    APPROVED,  // Đã phê duyệt (Tính vào trừ giờ theo QTN-10)
    REJECTED,  // Bị từ chối
    CANCELLED  // Đã hủy (QTN-22)
}
