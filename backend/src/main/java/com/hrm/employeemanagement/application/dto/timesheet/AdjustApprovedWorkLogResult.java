package com.hrm.employeemanagement.application.dto.timesheet;

import java.util.List;

/**
 * Kết quả sau khi điều chỉnh dòng giờ công đã duyệt kèm cảnh báo (nếu có).
 */
public record AdjustApprovedWorkLogResult(
        WorkLogResult entry,
        List<String> warnings
) {
}

