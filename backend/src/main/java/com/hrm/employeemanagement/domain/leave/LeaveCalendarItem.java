package com.hrm.employeemanagement.domain.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object đại diện cho một khoảng nghỉ của nhân viên trên lịch bộ phận.
 * Phục vụ User Story NCL-05-CN-006.
 */
public class LeaveCalendarItem {

    private final Long leaveRequestId;
    private final Long employeeId;
    private final String employeeCode;
    private final String fullName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LeaveStatus status;
    private final BigDecimal hoursDeducted;
    private final String leaveType;
    private final String reason;

    public LeaveCalendarItem(
            Long leaveRequestId,
            Long employeeId,
            String employeeCode,
            String fullName,
            LocalDate startDate,
            LocalDate endDate,
            LeaveStatus status,
            BigDecimal hoursDeducted,
            String leaveType,
            String reason
    ) {
        this.leaveRequestId = Objects.requireNonNull(leaveRequestId, "leaveRequestId must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.hoursDeducted = hoursDeducted != null ? hoursDeducted : BigDecimal.ZERO;
        this.leaveType = leaveType != null ? leaveType : "ANNUAL";
        this.reason = reason;
    }

    /**
     * Kiểm tra xem đơn nghỉ có bao gồm ngày được chỉ định hay không.
     */
    public boolean coversDate(LocalDate date) {
        if (date == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public Long getLeaveRequestId() {
        return leaveRequestId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public BigDecimal getHoursDeducted() {
        return hoursDeducted;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public String getReason() {
        return reason;
    }
}
