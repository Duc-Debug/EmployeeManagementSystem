package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ApproveEmployeeSkillRequest {

    @Min(value = 1, message = "Mức thành thạo phải từ 1 đến 5")
    @Max(value = 5, message = "Mức thành thạo phải từ 1 đến 5")
    private Integer adjustedProficiencyLevel;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String reviewNotes;

    public ApproveEmployeeSkillRequest() {
    }

    public ApproveEmployeeSkillRequest(Integer adjustedProficiencyLevel, String reviewNotes) {
        this.adjustedProficiencyLevel = adjustedProficiencyLevel;
        this.reviewNotes = reviewNotes;
    }

    public Integer getAdjustedProficiencyLevel() {
        return adjustedProficiencyLevel;
    }

    public void setAdjustedProficiencyLevel(Integer adjustedProficiencyLevel) {
        this.adjustedProficiencyLevel = adjustedProficiencyLevel;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}
