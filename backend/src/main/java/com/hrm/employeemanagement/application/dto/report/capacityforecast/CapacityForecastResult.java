package com.hrm.employeemanagement.application.dto.report.capacityforecast;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chứa kết quả báo cáo dự báo năng lực các tuần tới (NCL-10-CN-004).
 */
public record CapacityForecastResult(
        Long orgUnitId,
        String orgUnitName,
        int fromYear,
        int fromWeek,
        int durationWeeks,
        List<WeeklyForecastItem> weeks,
        CapacityForecastSummary summary,
        LocalDateTime generatedAt
) {

    /**
     * DTO chi tiết số liệu năng lực theo từng tuần ISO.
     */
    public record WeeklyForecastItem(
            int year,
            int weekNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal availableHours,
            BigDecimal committedHours,
            BigDecimal reservedHours,
            BigDecimal committedRemainingHours,
            BigDecimal projectedRemainingHours,
            BigDecimal committedUtilization,
            BigDecimal projectedUtilization,
            ForecastStatus status
    ) {
    }

    /**
     * DTO tổng kết báo cáo dự báo năng lực.
     */
    public record CapacityForecastSummary(
            BigDecimal totalAvailableHours,
            BigDecimal totalCommittedHours,
            BigDecimal totalReservedHours,
            BigDecimal totalProjectedRemainingHours,
            int overCapacityWeeks
    ) {
    }

    /**
     * Enum định nghĩa trạng thái năng lực tuần trong báo cáo dự báo:
     * - OVER_CAPACITY: Vượt năng lực (projectedRemainingHours < 0 hoặc utilization > 100%)
     * - NEAR_FULL: Gần đầy (utilization >= 80% và <= 100%)
     * - AVAILABLE: Còn trống (utilization < 80%)
     */
    public enum ForecastStatus {
        AVAILABLE,
        NEAR_FULL,
        OVER_CAPACITY
    }
}
