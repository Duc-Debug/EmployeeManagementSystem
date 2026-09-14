package com.hrm.employeemanagement.application.port.outbound.allocation;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;

/**
 * Output port dành cho thao tác xóa bản ghi phân bổ nguồn lực theo tuần (WeeklyProjectAllocation).
 * Phục vụ nguyên tắc kiến trúc phòng thủ (Defensive Architecture) theo tiêu chuẩn QTN-18.
 */
public interface DeleteWeeklyProjectAllocationPort {

    void delete(WeeklyProjectAllocation allocation);

    void deleteById(Long allocationId);
}
