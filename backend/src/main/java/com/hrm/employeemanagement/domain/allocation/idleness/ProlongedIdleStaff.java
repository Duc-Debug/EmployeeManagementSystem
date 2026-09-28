package com.hrm.employeemanagement.domain.allocation.idleness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain Aggregate / Entity biểu diễn nhân sự bị cảnh báo nhàn rỗi kéo dài.
 * Thuộc phạm vi NCL-07-CN-006 (QTN-23).
 */
public class ProlongedIdleStaff {

    private final Long employeeId;
    private final String employeeCode;
    private final String fullName;
    private final Long orgUnitId;
    private final String departmentName;
    private final String positionTitle;
    private final int consecutiveIdleWeeks;
    private final BigDecimal totalEmptyHours;
    private final BigDecimal averageUtilization;
    private final List<WeeklyIdlenessDetail> weeklyDetails;

    public ProlongedIdleStaff(
            Long employeeId,
            String employeeCode,
            String fullName,
            Long orgUnitId,
            String departmentName,
            String positionTitle,
            int consecutiveIdleWeeks,
            BigDecimal totalEmptyHours,
            BigDecimal averageUtilization,
            List<WeeklyIdlenessDetail> weeklyDetails
    ) {
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.employeeCode = Objects.requireNonNull(employeeCode, "employeeCode must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.orgUnitId = orgUnitId;
        this.departmentName = departmentName != null ? departmentName : "Chưa phân bổ";
        this.positionTitle = positionTitle != null ? positionTitle : "Chưa phân bổ";
        this.consecutiveIdleWeeks = consecutiveIdleWeeks;
        this.totalEmptyHours = totalEmptyHours != null ? totalEmptyHours : BigDecimal.ZERO;
        this.averageUtilization = averageUtilization != null ? averageUtilization : BigDecimal.ZERO;
        this.weeklyDetails = weeklyDetails != null ? List.copyOf(weeklyDetails) : Collections.emptyList();
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

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public int getConsecutiveIdleWeeks() {
        return consecutiveIdleWeeks;
    }

    public BigDecimal getTotalEmptyHours() {
        return totalEmptyHours;
    }

    public BigDecimal getAverageUtilization() {
        return averageUtilization;
    }

    public List<WeeklyIdlenessDetail> getWeeklyDetails() {
        return weeklyDetails;
    }
}
