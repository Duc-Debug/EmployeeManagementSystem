package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

public class RejectEmployeeSkillRequest {

    private String rejectionReason;

    public RejectEmployeeSkillRequest() {
    }

    public RejectEmployeeSkillRequest(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
