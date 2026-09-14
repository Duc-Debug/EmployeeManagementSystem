package com.hrm.employeemanagement.application.dto.allocation.idleness;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO kết quả của một nhân sự bị cảnh báo nhàn rỗi kéo dài (NCL-07-CN-006).
 */
public record ProlongedIdleStaffItemResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String departmentName,
        String positionTitle,
        int consecutiveIdleWeeks,
        BigDecimal totalEmptyHours,
        BigDecimal averageUtilization,
        List<ProlongedIdlenessWeeklyDetailResult> weeklyBreakdown
) {
}
