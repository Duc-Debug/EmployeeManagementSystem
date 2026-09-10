package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto;

import com.hrm.employeemanagement.domain.leave.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class SubmitLeaveRequestWebRequest {

    @NotNull(message = "Loại nghỉ phép không được để trống")
    private LeaveType leaveType;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @Size(max = 500, message = "Lý do nghỉ phép không được vượt quá 500 ký tự")
    private String reason;

    public SubmitLeaveRequestWebRequest() {}

    public SubmitLeaveRequestWebRequest(LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
