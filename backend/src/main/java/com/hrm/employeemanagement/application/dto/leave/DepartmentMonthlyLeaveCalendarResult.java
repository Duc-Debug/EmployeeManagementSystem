package com.hrm.employeemanagement.application.dto.leave;

import com.hrm.employeemanagement.domain.leave.DailyLeaveSummary;
import com.hrm.employeemanagement.domain.leave.DepartmentMonthlyLeaveCalendar;
import com.hrm.employeemanagement.domain.leave.LeaveCalendarItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result DTO trả về kết quả lịch nghỉ bộ phận theo tháng (NCL-05-CN-006).
 */
public record DepartmentMonthlyLeaveCalendarResult(
        Long orgUnitId,
        String orgUnitCode,
        String orgUnitName,
        int year,
        int month,
        int totalDepartmentEmployees,
        double warningThresholdPercentage,
        int totalLeaveRequests,
        int warningDaysCount,
        List<LeaveCalendarItemResult> leaveItems,
        List<DailyLeaveSummaryResult> dailySummaries
) {

    public record LeaveCalendarItemResult(
            Long leaveRequestId,
            Long employeeId,
            String employeeCode,
            String fullName,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            BigDecimal hoursDeducted,
            String leaveType,
            String reason
    ) {
        public static LeaveCalendarItemResult from(LeaveCalendarItem item) {
            return new LeaveCalendarItemResult(
                    item.getLeaveRequestId(),
                    item.getEmployeeId(),
                    item.getEmployeeCode(),
                    item.getFullName(),
                    item.getStartDate(),
                    item.getEndDate(),
                    item.getStatus().name(),
                    item.getHoursDeducted(),
                    item.getLeaveType(),
                    item.getReason()
            );
        }
    }

    public record DailyLeaveSummaryResult(
            LocalDate date,
            String dayOfWeek,
            int totalOnLeave,
            int approvedCount,
            int pendingCount,
            boolean isWarning,
            String warningMessage,
            List<LeaveCalendarItemResult> leaveItems
    ) {
        public static DailyLeaveSummaryResult from(DailyLeaveSummary summary) {
            List<LeaveCalendarItemResult> items = summary.getLeaveItems().stream()
                    .map(LeaveCalendarItemResult::from)
                    .toList();
            return new DailyLeaveSummaryResult(
                    summary.getDate(),
                    summary.getDayOfWeek().name(),
                    summary.getTotalOnLeave(),
                    summary.getApprovedCount(),
                    summary.getPendingCount(),
                    summary.isWarning(),
                    summary.getWarningMessage(),
                    items
            );
        }
    }

    public static DepartmentMonthlyLeaveCalendarResult from(DepartmentMonthlyLeaveCalendar calendar) {
        List<LeaveCalendarItemResult> items = calendar.getLeaveItems().stream()
                .map(LeaveCalendarItemResult::from)
                .toList();

        List<DailyLeaveSummaryResult> summaries = calendar.getDailySummaries().stream()
                .map(DailyLeaveSummaryResult::from)
                .toList();

        return new DepartmentMonthlyLeaveCalendarResult(
                calendar.getOrgUnitId(),
                calendar.getOrgUnitCode(),
                calendar.getOrgUnitName(),
                calendar.getYear(),
                calendar.getMonth(),
                calendar.getTotalDepartmentEmployees(),
                calendar.getWarningThresholdPercentage(),
                calendar.getTotalLeaveRequests(),
                calendar.getWarningDaysCount(),
                items,
                summaries
        );
    }
}
