package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.util.List;

import com.hrm.employeemanagement.domain.notification.Notification;

public interface LoadAllocationNotificationPort {

    /**
     * Tìm danh sách thông báo phân bổ theo danh sách projectId (hoặc tất cả nếu rỗng).
     */
    List<Notification> findAllocationNotifications(List<Long> projectIds, int page, int size);

    /**
     * Đếm tổng số thông báo phân bổ theo danh sách projectId.
     */
    long countAllocationNotifications(List<Long> projectIds);
}
