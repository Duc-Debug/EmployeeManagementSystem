package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto;

import com.hrm.employeemanagement.application.dto.leave.DepartmentMonthlyLeaveCalendarResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST API Response DTO cho lịch nghỉ của bộ phận theo tháng (NCL-05-CN-006).
 */
public record DepartmentMonthlyLeaveCalendarResponse(
        Long orgUnitId,
        String orgUnitCode,
        String orgUnitName,
        int year,
        int month,
        int totalDepartmentEmployees,
        double warningThresholdPercentage,
        int totalLeaveRequests,
        int warningDaysCount,
        List<LeaveCalendarItemResponse> leaveItems,
        List<DailyLeaveSummaryResponse> dailySummaries
) {

    public record LeaveCalendarItemResponse(
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
        public static LeaveCalendarItemResponse from(DepartmentMonthlyLeaveCalendarResult.LeaveCalendarItemResult item) {
            return new LeaveCalendarItemResponse(
                    item.leaveRequestId(),
                    item.employeeId(),
                    item.employeeCode(),
                    item.fullName(),
                    item.startDate(),
                    item.endDate(),
                    item.status(),
                    item.hoursDeducted(),
                    item.leaveType(),
                    item.reason()
            );
        }
    }

    public record DailyLeaveSummaryResponse(
            LocalDate date,
            String dayOfWeek,
            int totalOnLeave,
            int approvedCount,
            int pendingCount,
            boolean isWarning,
            String warningMessage,
            List<LeaveCalendarItemResponse> leaveItems
    ) {
        public static DailyLeaveSummaryResponse from(DepartmentMonthlyLeaveCalendarResult.DailyLeaveSummaryResult summary) {
            List<LeaveCalendarItemResponse> items = summary.leaveItems().stream()
                    .map(LeaveCalendarItemResponse::from)
                    .toList();
            return new DailyLeaveSummaryResponse(
                    summary.date(),
                    summary.dayOfWeek(),
                    summary.totalOnLeave(),
                    summary.approvedCount(),
                    summary.pendingCount(),
                    summary.isWarning(),
                    summary.warningMessage(),
                    items
            );
        }
    }

    public static DepartmentMonthlyLeaveCalendarResponse from(DepartmentMonthlyLeaveCalendarResult result) {
        List<LeaveCalendarItemResponse> items = result.leaveItems().stream()
                .map(LeaveCalendarItemResponse::from)
                .toList();

        List<DailyLeaveSummaryResponse> summaries = result.dailySummaries().stream()
                .map(DailyLeaveSummaryResponse::from)
                .toList();

        return new DepartmentMonthlyLeaveCalendarResponse(
                result.orgUnitId(),
                result.orgUnitCode(),
                result.orgUnitName(),
                result.year(),
                result.month(),
                result.totalDepartmentEmployees(),
                result.warningThresholdPercentage(),
                result.totalLeaveRequests(),
                result.warningDaysCount(),
                items,
                summaries
        );
    }
}
