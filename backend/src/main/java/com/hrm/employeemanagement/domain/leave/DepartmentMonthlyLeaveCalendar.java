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
        return calculate(
                orgUnitId,
                orgUnitCode,
                orgUnitName,
                year,
                month,
                totalDepartmentEmployees,
                customThresholdRate,
                allLeaveItems,
                null,
                null
        );
    }

    /**
     * Factory method nâng cao nhận diện ngày làm việc công ty và ngày lễ (Cải tiến P1 & P2).
     */
    public static DepartmentMonthlyLeaveCalendar calculate(
            Long orgUnitId,
            String orgUnitCode,
            String orgUnitName,
            int year,
            int month,
            int totalDepartmentEmployees,
            Double customThresholdRate,
            List<LeaveCalendarItem> allLeaveItems,
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar companyCalendar,
            Set<LocalDate> holidayDates
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();

        List<LeaveCalendarItem> items = allLeaveItems != null ? allLeaveItems : Collections.emptyList();
        List<DailyLeaveSummary> summaries = new ArrayList<>(daysInMonth);
        int warningCount = 0;

        double appliedThreshold = (customThresholdRate != null && customThresholdRate > 0 && customThresholdRate <= 1.0)
                ? customThresholdRate
                : LeaveThresholdPolicy.DEFAULT_WARNING_THRESHOLD_PERCENTAGE;

        // Phân bổ chính xác số giờ nghỉ theo từng ngày làm việc thực tế cho mỗi đơn nghỉ (Review Issue 2)
        java.util.Map<LeaveCalendarItem, java.math.BigDecimal> dailyHoursByItem = new java.util.HashMap<>();
        for (LeaveCalendarItem item : items) {
            java.math.BigDecimal totalHours = item.getHoursDeducted() != null ? item.getHoursDeducted() : java.math.BigDecimal.ZERO;
            int reqWorkingDays = LeaveRequestPolicy.calculateWorkingDays(
                    item.getStartDate(),
                    item.getEndDate(),
                    companyCalendar,
                    holidayDates
            );
            java.math.BigDecimal dailyHours;
            if (reqWorkingDays > 0) {
                dailyHours = totalHours.divide(java.math.BigDecimal.valueOf(reqWorkingDays), 2, java.math.RoundingMode.HALF_UP);
            } else {
                dailyHours = totalHours;
            }
            dailyHoursByItem.put(item, dailyHours);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = yearMonth.atDay(day);

            List<LeaveCalendarItem> dayItems = new ArrayList<>();
            Set<Long> employeesOnLeave = new HashSet<>();
            int approvedCount = 0;
            int pendingCount = 0;
            java.math.BigDecimal totalLeaveHoursInDay = java.math.BigDecimal.ZERO;

            // Nhận diện ngày làm việc công ty & ngày lễ
            boolean isWorkingDay = (companyCalendar == null)
                    ? (currentDate.getDayOfWeek() != java.time.DayOfWeek.SATURDAY && currentDate.getDayOfWeek() != java.time.DayOfWeek.SUNDAY)
                    : companyCalendar.isWorkingDay(currentDate.getDayOfWeek());

            boolean isHoliday = (holidayDates != null) && holidayDates.contains(currentDate);
            boolean isCompanyWorkingDay = isWorkingDay && !isHoliday;

            for (LeaveCalendarItem item : items) {
                if (item.coversDate(currentDate)) {
                    dayItems.add(item);
                    employeesOnLeave.add(item.getEmployeeId());
                    if (item.getStatus() == LeaveStatus.APPROVED) {
                        approvedCount++;
                    } else if (item.getStatus() == LeaveStatus.PENDING) {
                        pendingCount++;
                    }
                    // Chỉ cộng giờ nghỉ vào ngày làm việc thực tế của công ty (không tính vào T7/CN/Lễ)
                    if (isCompanyWorkingDay) {
                        java.math.BigDecimal dailyHours = dailyHoursByItem.getOrDefault(item, java.math.BigDecimal.ZERO);
                        totalLeaveHoursInDay = totalLeaveHoursInDay.add(dailyHours);
                    }
                }
            }

            int distinctOnLeave = employeesOnLeave.size();

            // Bỏ qua cảnh báo nếu là ngày nghỉ cuối tuần hoặc ngày lễ (P1)
            boolean isWarning = LeaveThresholdPolicy.isWarningExceeded(
                    totalDepartmentEmployees,
                    distinctOnLeave,
                    appliedThreshold,
                    isCompanyWorkingDay
            );

            String warningMessage = isWarning
                    ? LeaveThresholdPolicy.buildWarningMessage(currentDate, totalDepartmentEmployees, distinctOnLeave, appliedThreshold, isCompanyWorkingDay)
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
                    isCompanyWorkingDay,
                    isHoliday,
                    totalLeaveHoursInDay,
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
