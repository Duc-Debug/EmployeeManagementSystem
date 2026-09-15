package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.util.List;

import com.hrm.employeemanagement.domain.notification.Notification;

public interface LoadAllocationNotificationPort {

    /**
     * Tìm danh sách thông báo phân bổ theo người nhận (tùy chọn) và danh sách projectId.
     * An toàn mặc định: Nếu recipientId == null và danh sách projectId rỗng hoặc null, trả về danh sách rỗng.
     */
    List<Notification> findAllocationNotifications(Long recipientId, List<Long> projectIds, int page, int size);

    /**
     * Đếm tổng số thông báo phân bổ theo người nhận (tùy chọn) và danh sách projectId.
     * An toàn mặc định: Nếu recipientId == null và danh sách projectId rỗng hoặc null, trả về 0.
     */
    long countAllocationNotifications(Long recipientId, List<Long> projectIds);

    /**
     * Tìm danh sách thông báo phân bổ toàn công ty (không giới hạn dự án/người nhận).
     * Chỉ được gọi khi người dùng có phạm vi dữ liệu toàn công ty (DataScope.COMPANY).
     */
    List<Notification> findAllCompanyAllocationNotifications(int page, int size);

    /**
     * Đếm tổng số thông báo phân bổ toàn công ty.
     */
    long countAllCompanyAllocationNotifications();
}
