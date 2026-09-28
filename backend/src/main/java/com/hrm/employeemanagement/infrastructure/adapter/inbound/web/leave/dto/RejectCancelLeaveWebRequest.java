package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectCancelLeaveWebRequest {

    @NotBlank(message = "Lý do từ chối yêu cầu hủy đơn nghỉ phép không được để trống")
    @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
    private String reason;

    public RejectCancelLeaveWebRequest() {}

    public RejectCancelLeaveWebRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
