package com.hrm.employeemanagement.domain.unavailability;

public enum UnavailabilityStatus {
    PENDING,    // Chờ quản lý nguồn lực phê duyệt
    APPROVED,   // Đã phê duyệt (Trừ giờ khả dụng tuần)
    REJECTED,   // Bị từ chối
    CANCELLED   // Đã hủy bởi người khai báo
}
