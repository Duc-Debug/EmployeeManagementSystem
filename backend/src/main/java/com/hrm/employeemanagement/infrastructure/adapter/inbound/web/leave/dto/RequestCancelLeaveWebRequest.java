package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequestCancelLeaveWebRequest {

    @NotBlank(message = "Lý do yêu cầu hủy đơn nghỉ phép không được để trống")
    @Size(max = 500, message = "Lý do yêu cầu hủy không được vượt quá 500 ký tự")
    private String reason;

    public RequestCancelLeaveWebRequest() {}

    public RequestCancelLeaveWebRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
