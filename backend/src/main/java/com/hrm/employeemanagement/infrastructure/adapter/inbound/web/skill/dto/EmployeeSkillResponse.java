package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;

public class EmployeeSkillResponse {

    private Long id;
    private Long employeeId;
    private Long skillId;
    private String skillName;
    private String skillCode;
    private String skillCategory;
    private Integer proficiencyLevel;
    private Double yearsOfExperience;
    private String status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmployeeSkillResponse() {
    }

    public static EmployeeSkillResponse fromResult(EmployeeSkillResult result) {
        EmployeeSkillResponse response = new EmployeeSkillResponse();
        response.id = result.id();
        response.employeeId = result.employeeId();
        response.skillId = result.skillId();
        response.skillName = result.skillName();
        response.skillCode = result.skillCode();
        response.skillCategory = result.skillCategory();
        response.proficiencyLevel = result.proficiencyLevel();
        response.yearsOfExperience = result.yearsOfExperience();
        response.status = result.status();
        response.approvedBy = result.approvedBy();
        response.approvedAt = result.approvedAt();
        response.rejectionReason = result.rejectionReason();
        response.createdAt = result.createdAt();
        response.updatedAt = result.updatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public String getSkillCategory() {
        return skillCategory;
    }

    public Integer getProficiencyLevel() {
        return proficiencyLevel;
    }

    public Double getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getStatus() {
        return status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
