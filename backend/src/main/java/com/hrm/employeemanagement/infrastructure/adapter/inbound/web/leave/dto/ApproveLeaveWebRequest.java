package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto;

public class ApproveLeaveWebRequest {

    private String comment;

    public ApproveLeaveWebRequest() {}

    public ApproveLeaveWebRequest(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
