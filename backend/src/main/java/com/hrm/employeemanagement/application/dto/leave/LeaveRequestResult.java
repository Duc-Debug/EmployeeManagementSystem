package com.hrm.employeemanagement.application.dto.leave;

import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResult(
        Long id,
        Long employeeId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        int daysCount,
        BigDecimal hoursDeducted,
        String reason,
        LeaveStatus status,
        Long approverId,
        String approverComment,
        String cancellationReason,
        LocalDateTime cancellationRequestedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String employeeName,
        String orgUnitName
) {
    public LeaveRequestResult(
            Long id,
            Long employeeId,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            int daysCount,
            BigDecimal hoursDeducted,
            String reason,
            LeaveStatus status,
            Long approverId,
            String approverComment,
            String cancellationReason,
            LocalDateTime cancellationRequestedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, employeeId, leaveType, startDate, endDate, daysCount, hoursDeducted, reason, status,
                approverId, approverComment, cancellationReason, cancellationRequestedAt, createdAt, updatedAt,
                null, null);
    }

    public static LeaveRequestResult fromDomain(LeaveRequest domain) {
        return fromDomain(domain, null, null);
    }

    public static LeaveRequestResult fromDomain(LeaveRequest domain, String employeeName, String orgUnitName) {
        return new LeaveRequestResult(
                domain.getId(),
                domain.getEmployeeId(),
                domain.getLeaveType(),
                domain.getStartDate(),
                domain.getEndDate(),
                domain.getDaysCount(),
                domain.getHoursDeducted(),
                domain.getReason(),
                domain.getStatus(),
                domain.getApproverId(),
                domain.getApproverComment(),
                domain.getCancellationReason(),
                domain.getCancellationRequestedAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                employeeName,
                orgUnitName
        );
    }
}
