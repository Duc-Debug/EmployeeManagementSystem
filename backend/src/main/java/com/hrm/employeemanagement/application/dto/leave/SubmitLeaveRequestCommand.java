package com.hrm.employeemanagement.application.dto.leave;

import com.hrm.employeemanagement.domain.leave.LeaveType;

import java.time.LocalDate;

/**
 * Command chứa dữ liệu gửi đơn xin nghỉ phép (NCL-05-CN-002).
 */
public record SubmitLeaveRequestCommand(
        Long employeeId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
}
