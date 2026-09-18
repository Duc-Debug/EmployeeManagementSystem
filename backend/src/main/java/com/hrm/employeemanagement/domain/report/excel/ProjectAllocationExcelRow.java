package com.hrm.employeemanagement.domain.report.excel;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Dòng dữ liệu chi tiết của từng nhân sự trong Báo cáo phân bổ dự án xuất Excel.
 * Đã áp dụng quy tắc che thông tin nhạy cảm.
 */
public class ProjectAllocationExcelRow {

    private final Long employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String projectRole;
    private final String orgUnitName;
    private final Map<YearWeek, BigDecimal> weeklyHours;
    private final BigDecimal totalHours;
    private final String maskedSalary;
    private final String maskedCostRate;

    public ProjectAllocationExcelRow(
            Long employeeId,
            String employeeCode,
            String fullName,
            String projectRole,
            String orgUnitName,
            Map<YearWeek, BigDecimal> weeklyHours,
            BigDecimal totalHours,
            String maskedSalary,
            String maskedCostRate
    ) {
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId không được null");
        this.employeeCode = Objects.requireNonNull(employeeCode, "employeeCode không được null");
        this.fullName = Objects.requireNonNull(fullName, "fullName không được null");
        this.projectRole = projectRole != null ? projectRole : "Thành viên";
        this.orgUnitName = orgUnitName != null ? orgUnitName : "Chưa gán";
        this.weeklyHours = weeklyHours != null ? Map.copyOf(weeklyHours) : Collections.emptyMap();
        this.totalHours = totalHours != null ? totalHours : BigDecimal.ZERO;
        this.maskedSalary = maskedSalary != null ? maskedSalary : "***";
        this.maskedCostRate = maskedCostRate != null ? maskedCostRate : "***";
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getProjectRole() {
        return projectRole;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public Map<YearWeek, BigDecimal> getWeeklyHours() {
        return weeklyHours;
    }

    public BigDecimal getHoursForWeek(YearWeek yw) {
        return weeklyHours.getOrDefault(yw, BigDecimal.ZERO);
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public String getMaskedSalary() {
        return maskedSalary;
    }

    public String getMaskedCostRate() {
        return maskedCostRate;
    }
}
