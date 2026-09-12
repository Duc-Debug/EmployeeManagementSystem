package com.hrm.employeemanagement.application.dto.allocation;

import com.hrm.employeemanagement.domain.allocation.CapacityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompanyWeeklyCapacityMatrixResult(
        Long orgUnitId,
        String orgUnitName,
        int fromYear,
        int fromWeek,
        int durationWeeks,
        List<HeaderWeekInfo> weeks,
        List<EmployeeCapacityRowResult> rows,
        CapacityMatrixSummaryResult summary,
        int page,
        int pageSize,
        int totalEmployees,
        int totalPages
) {

    public CompanyWeeklyCapacityMatrixResult(
            Long orgUnitId,
            String orgUnitName,
            int fromYear,
            int fromWeek,
            int durationWeeks,
            List<HeaderWeekInfo> weeks,
            List<EmployeeCapacityRowResult> rows,
            CapacityMatrixSummaryResult summary
    ) {
        this(
                orgUnitId,
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                weeks,
                rows,
                summary,
                0,
                rows != null ? Math.max(1, rows.size()) : 20,
                rows != null ? rows.size() : 0,
                rows != null && !rows.isEmpty() ? 1 : 0
        );
    }

    public record HeaderWeekInfo(
            int year,
            int weekNumber,
            LocalDate startDate,
            LocalDate endDate,
            String label
    ) {}

    public record CapacityMatrixCellResult(
            int year,
            int weekNumber,
            BigDecimal allocatedHours,
            BigDecimal availableHours,
            BigDecimal remainingHours,
            BigDecimal utilizationPercentage,
            boolean isOverloaded,
            BigDecimal excessHours,
            CapacityStatus status,
            BigDecimal reservedHours
    ) {
        public CapacityMatrixCellResult(
                int year,
                int weekNumber,
                BigDecimal allocatedHours,
                BigDecimal availableHours,
                BigDecimal remainingHours,
                BigDecimal utilizationPercentage,
                boolean isOverloaded,
                BigDecimal excessHours,
                CapacityStatus status
        ) {
            this(year, weekNumber, allocatedHours, availableHours, remainingHours, utilizationPercentage, isOverloaded, excessHours, status, BigDecimal.ZERO);
        }
    }

    public record EmployeeCapacityRowResult(
            Long employeeId,
            String employeeCode,
            String fullName,
            Long orgUnitId,
            String orgUnitName,
            String professionalRole,
            List<CapacityMatrixCellResult> cells,
            BigDecimal totalAllocatedHours,
            BigDecimal totalAvailableHours,
            BigDecimal averageUtilization,
            int overloadedWeeksCount
    ) {}

    /**
     * Chỉ số thống kê KPI được tính toán trên phạm vi lát cắt của trang hiện tại (Page-scoped Summary)
     * để đảm bảo nhất quán với kết quả phân trang và không pha trộn số liệu toàn cục với cục bộ.
     */
    public record CapacityMatrixSummaryResult(
            int pageEmployeesCount,
            int totalWeeks,
            int overloadedEmployeesCount,
            int overloadedCellsCount,
            int underutilizedCellsCount,
            BigDecimal averageUtilization
    ) {
        // Bí danh để tương thích ngược
        public int totalEmployees() {
            return pageEmployeesCount;
        }
    }
}
