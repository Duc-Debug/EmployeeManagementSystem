package com.hrm.employeemanagement.application.port.inbound.allocation;

import com.hrm.employeemanagement.application.dto.allocation.AllocationNotificationPageResult;

public interface GetAllocationNotificationsUseCase {

    /**
     * Lấy danh sách thông báo phân bổ có phân trang và kiểm tra phân quyền (BR-05, AC-03).
     *
     * @param projectId mã dự án cần lọc (tùy chọn)
     * @param page      số trang (0-indexed)
     * @param size      kích thước trang
     * @return kết quả danh sách thông báo phân bổ
     */
    AllocationNotificationPageResult getAllocationNotifications(Long projectId, int page, int size);
}
