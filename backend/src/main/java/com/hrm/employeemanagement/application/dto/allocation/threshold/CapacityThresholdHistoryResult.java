package com.hrm.employeemanagement.application.dto.allocation.threshold;

import java.time.LocalDateTime;

/**
 * Result DTO biểu diễn một bản ghi lịch sử thay đổi cấu hình ngưỡng phục vụ kiểm toán (TC-04).
 */
public record CapacityThresholdHistoryResult(
        Long id,
        Long userId,
        String userName,
        String action,
        String oldValue,
        String newValue,
        LocalDateTime createdAt
) {
}
