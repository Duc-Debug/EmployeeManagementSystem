package com.hrm.employeemanagement.application.dto.allocation.idleness;

import java.util.Objects;

/**
 * Command DTO xác nhận xử lý cảnh báo nhân sự nhàn rỗi (NCL-07-CN-006-TC-04).
 */
public record AcknowledgeProlongedIdleStaffCommand(
        Long employeeId,
        String actionTaken,
        String notes
) {
    public AcknowledgeProlongedIdleStaffCommand {
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(actionTaken, "actionTaken must not be null");
    }
}
