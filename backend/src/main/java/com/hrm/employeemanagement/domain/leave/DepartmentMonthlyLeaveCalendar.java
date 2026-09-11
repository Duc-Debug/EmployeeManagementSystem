package com.hrm.employeemanagement.domain.leave;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Domain Aggregate biểu diễn Lịch nghỉ phép của cả bộ phận theo tháng (NCL-05-CN-006).
 */
public class DepartmentMonthlyLeaveCalendar {

    private final Long orgUnitId;
    private final String orgUnitCode;
    private final String orgUnitName;
    private final int year;
    private final int month;
    private final int totalDepartmentEmployees;
    private final double warningThresholdPercentage;
    private final int totalLeaveRequests;
    private final int warningDaysCount;
    private final List<LeaveCalendarItem> leaveItems;
    private final List<DailyLeaveSummary> dailySummaries;

    public DepartmentMonthlyLeaveCalendar(
            Long orgUnitId,
            String orgUnitCode,
            String orgUnitName,
            int year,
            int month,
            int totalDepartmentEmployees,
            double warningThresholdPercentage,
            int totalLeaveRequests,
            int warningDaysCount,
            List<LeaveCalendarItem> leaveItems,
            List<DailyLeaveSummary> dailySummaries
    ) {
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId must not be null");
        this.orgUnitCode = orgUnitCode;
        this.orgUnitName = orgUnitName;
        this.year = year;
        this.month = month;
        this.totalDepartmentEmployees = totalDepartmentEmployees;
        this.warningThresholdPercentage = warningThresholdPercentage;
        this.totalLeaveRequests = totalLeaveRequests;
        this.warningDaysCount = warningDaysCount;
        this.leaveItems = leaveItems != null ? Collections.unmodifiableList(leaveItems) : Collections.emptyList();
        this.dailySummaries = dailySummaries != null ? Collections.unmodifiableList(dailySummaries) : Collections.emptyList();
    }

    /**
     * Factory method tổng hợp và dựng lịch nghỉ tháng của bộ phận.
     */
    public static DepartmentMonthlyLeaveCalendar calculate(
            Long orgUnitId,
            String orgUnitCode,
            String orgUnitName,
            int year,
            int month,
            int totalDepartmentEmployees,
            Double customThresholdRate,
            List<LeaveCalendarItem> allLeaveItems
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();

        List<LeaveCalendarItem> items = allLeaveItems != null ? allLeaveItems : Collections.emptyList();
        List<DailyLeaveSummary> summaries = new ArrayList<>(daysInMonth);
        int warningCount = 0;

        double appliedThreshold = (customThresholdRate != null && customThresholdRate > 0 && customThresholdRate <= 1.0)
                ? customThresholdRate
                : LeaveThresholdPolicy.DEFAULT_WARNING_THRESHOLD_PERCENTAGE;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = yearMonth.atDay(day);

            List<LeaveCalendarItem> dayItems = new ArrayList<>();
            Set<Long> employeesOnLeave = new HashSet<>();
            int approvedCount = 0;
            int pendingCount = 0;

            for (LeaveCalendarItem item : items) {
                if (item.coversDate(currentDate)) {
                    dayItems.add(item);
                    employeesOnLeave.add(item.getEmployeeId());
                    if (item.getStatus() == LeaveStatus.APPROVED) {
                        approvedCount++;
                    } else if (item.getStatus() == LeaveStatus.PENDING) {
                        pendingCount++;
                    }
                }
            }

            int distinctOnLeave = employeesOnLeave.size();
            boolean isWarning = LeaveThresholdPolicy.isWarningExceeded(
                    totalDepartmentEmployees,
                    distinctOnLeave,
                    appliedThreshold
            );

            String warningMessage = isWarning
                    ? LeaveThresholdPolicy.buildWarningMessage(currentDate, totalDepartmentEmployees, distinctOnLeave, appliedThreshold)
                    : null;

            if (isWarning) {
                warningCount++;
            }

            summaries.add(new DailyLeaveSummary(
                    currentDate,
                    currentDate.getDayOfWeek(),
                    distinctOnLeave,
                    approvedCount,
                    pendingCount,
                    isWarning,
                    warningMessage,
                    dayItems
            ));
        }

        return new DepartmentMonthlyLeaveCalendar(
                orgUnitId,
                orgUnitCode,
                orgUnitName,
                year,
                month,
                totalDepartmentEmployees,
                appliedThreshold,
                items.size(),
                warningCount,
                items,
                summaries
        );
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getOrgUnitCode() {
        return orgUnitCode;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getTotalDepartmentEmployees() {
        return totalDepartmentEmployees;
    }

    public double getWarningThresholdPercentage() {
        return warningThresholdPercentage;
    }

    public int getTotalLeaveRequests() {
        return totalLeaveRequests;
    }

    public int getWarningDaysCount() {
        return warningDaysCount;
    }

    public List<LeaveCalendarItem> getLeaveItems() {
        return leaveItems;
    }

    public List<DailyLeaveSummary> getDailySummaries() {
        return dailySummaries;
    }
}
