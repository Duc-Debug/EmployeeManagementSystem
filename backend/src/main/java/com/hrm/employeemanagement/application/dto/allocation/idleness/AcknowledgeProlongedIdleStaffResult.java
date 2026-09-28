package com.hrm.employeemanagement.application.dto.allocation.idleness;

import java.time.LocalDateTime;

/**
 * Result DTO sau khi xác nhận xử lý cảnh báo nhàn rỗi kéo dài (NCL-07-CN-006-TC-04).
 */
public record AcknowledgeProlongedIdleStaffResult(
        Long employeeId,
        String employeeName,
        String actionTaken,
        String notes,
        Long acknowledgedBy,
        LocalDateTime acknowledgedAt,
        String status
) {
}
