package com.hrm.employeemanagement.domain.leave;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value Object biểu diễn thống kê nghỉ phép của bộ phận trong một ngày cụ thể.
 * Phục vụ User Story NCL-05-CN-006 & TC-02.
 */
public class DailyLeaveSummary {

    private final LocalDate date;
    private final DayOfWeek dayOfWeek;
    private final int totalOnLeave;
    private final int approvedCount;
    private final int pendingCount;
    private final boolean isWarning;
    private final String warningMessage;
    private final List<LeaveCalendarItem> leaveItems;

    public DailyLeaveSummary(
            LocalDate date,
            DayOfWeek dayOfWeek,
            int totalOnLeave,
            int approvedCount,
            int pendingCount,
            boolean isWarning,
            String warningMessage,
            List<LeaveCalendarItem> leaveItems
    ) {
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.dayOfWeek = dayOfWeek != null ? dayOfWeek : date.getDayOfWeek();
        this.totalOnLeave = totalOnLeave;
        this.approvedCount = approvedCount;
        this.pendingCount = pendingCount;
        this.isWarning = isWarning;
        this.warningMessage = warningMessage;
        this.leaveItems = leaveItems != null ? Collections.unmodifiableList(leaveItems) : Collections.emptyList();
    }

    public LocalDate getDate() {
        return date;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public int getTotalOnLeave() {
        return totalOnLeave;
    }

    public int getApprovedCount() {
        return approvedCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public boolean isWarning() {
        return isWarning;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public List<LeaveCalendarItem> getLeaveItems() {
        return leaveItems;
    }
}
