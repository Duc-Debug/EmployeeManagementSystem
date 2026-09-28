package com.hrm.employeemanagement.application.dto.timesheet;

import java.math.BigDecimal;

/**
 * Lệnh điều chỉnh dòng ghi giờ công đã duyệt.
 */
public record AdjustApprovedWorkLogCommand(
        Long entryId,
        BigDecimal hours,
        Long taskId,
        Boolean billable,
        String description,
        String reason,
        Long version
) {
}

