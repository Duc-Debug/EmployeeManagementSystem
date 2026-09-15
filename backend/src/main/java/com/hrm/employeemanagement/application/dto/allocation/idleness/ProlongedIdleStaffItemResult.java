package com.hrm.employeemanagement.application.dto.allocation.idleness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        List<ProlongedIdlenessWeeklyDetailResult> weeklyBreakdown,
        String status,
        String actionTaken,
        String ackNotes,
        LocalDateTime acknowledgedAt,
        Long acknowledgedBy
) {
    public ProlongedIdleStaffItemResult(
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
        this(
                employeeId,
                employeeCode,
                fullName,
                orgUnitId,
                departmentName,
                positionTitle,
                consecutiveIdleWeeks,
                totalEmptyHours,
                averageUtilization,
                weeklyBreakdown,
                "OPEN",
                null,
                null,
                null,
                null
        );
    }
}
