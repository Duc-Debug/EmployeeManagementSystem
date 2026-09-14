package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.util.List;

import com.hrm.employeemanagement.domain.notification.Notification;

public interface LoadAllocationNotificationPort {

    /**
     * Tìm danh sách thông báo phân bổ theo người nhận (tùy chọn) và danh sách projectId (hoặc tất cả nếu rỗng).
     */
    List<Notification> findAllocationNotifications(Long recipientId, List<Long> projectIds, int page, int size);

    /**
     * Đếm tổng số thông báo phân bổ theo người nhận (tùy chọn) và danh sách projectId.
     */
    long countAllocationNotifications(Long recipientId, List<Long> projectIds);
}
