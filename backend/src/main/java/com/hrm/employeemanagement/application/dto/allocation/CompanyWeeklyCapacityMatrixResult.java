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
        CapacityMatrixSummaryResult summary
) {

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
            CapacityStatus status
    ) {}

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

    public record CapacityMatrixSummaryResult(
            int totalEmployees,
            int totalWeeks,
            int overloadedEmployeesCount,
            int overloadedCellsCount,
            int underutilizedCellsCount,
            BigDecimal averageUtilization
    ) {}
}
