package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto;

import jakarta.validation.constraints.Size;

public class ApproveCancelLeaveWebRequest {

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String comment;

    public ApproveCancelLeaveWebRequest() {}

    public ApproveCancelLeaveWebRequest(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
