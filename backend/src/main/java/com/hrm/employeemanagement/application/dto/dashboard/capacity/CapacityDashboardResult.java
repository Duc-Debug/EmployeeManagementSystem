package com.hrm.employeemanagement.application.dto.dashboard.capacity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO kết quả Bảng điều khiển năng lực (NCL-10-CN-001).
 * Cung cấp 5 chỉ số cốt lõi và các danh mục phân tích phục vụ Ban Giám Đốc và Quản lý.
 */
public record CapacityDashboardResult(
        Long orgUnitId,
        String orgUnitName,
        int fromYear,
        int fromWeek,
        int durationWeeks,
        BigDecimal averageCapacityUtilization,      // KPI 1: Tỷ lệ sử dụng năng lực trung bình (%)
        int overloadedEmployeesCount,              // KPI 2: Số người quá tải
        BigDecimal departmentFreeHours,            // KPI 3: Số giờ còn rảnh của bộ phận/toàn công ty
        int unresolvedScheduleConflictsCount,      // KPI 4: Số xung đột lịch chưa xử lý
        long activeProjectsCount,                  // KPI 5: Số dự án đang chạy
        BigDecimal totalAvailableHours,            // Tổng giờ khả dụng ròng trong kỳ
        BigDecimal totalAllocatedHours,            // Tổng giờ phân bổ cam kết trong kỳ
        List<WeeklyCapacityDashboardItem> weeklyMetrics,
        List<DepartmentCapacityItem> departmentBreakdown,
        List<OverloadedEmployeeItem> overloadedEmployees,
        List<UnresolvedConflictItem> unresolvedConflicts,
        List<ActiveProjectSummaryItem> activeProjects,
        LocalDateTime generatedAt
) {
    public record WeeklyCapacityDashboardItem(
            int year,
            int weekNumber,
            LocalDate startDate,
            LocalDate endDate,
            String label,
            BigDecimal availableHours,
            BigDecimal allocatedHours,
            BigDecimal remainingHours,
            BigDecimal utilizationRate,
            int overloadedEmployeesCount
    ) {}

    public record DepartmentCapacityItem(
            Long orgUnitId,
            String orgUnitName,
            int employeeCount,
            BigDecimal allocatedHours,
            BigDecimal availableHours,
            BigDecimal freeHours,
            BigDecimal utilizationRate,
            int overloadedEmployeesCount
    ) {}

    public record OverloadedEmployeeItem(
            Long employeeId,
            String employeeCode,
            String fullName,
            Long orgUnitId,
            String orgUnitName,
            String professionalRole,
            BigDecimal totalAllocatedHours,
            BigDecimal totalAvailableHours,
            BigDecimal averageUtilizationRate,
            int overloadedWeeksCount
    ) {}

    public record UnresolvedConflictItem(
            Long conflictId,
            Long employeeId,
            String employeeCode,
            String employeeName,
            String conflictType,
            int yearNumber,
            int weekNumber,
            int conflictingProjectsCount,
            BigDecimal totalAllocatedHours,
            String status,
            String details
    ) {}

    public record ActiveProjectSummaryItem(
            Long projectId,
            String projectCode,
            String projectName,
            Long orgUnitId,
            String orgUnitName,
            String pmName,
            String startDate,
            String endDate,
            Integer estimatedHours,
            int memberCount,
            String status
    ) {}
}
