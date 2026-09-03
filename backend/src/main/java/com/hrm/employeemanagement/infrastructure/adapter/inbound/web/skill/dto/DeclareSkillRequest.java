package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DeclareSkillRequest {

    @NotNull(message = "ID kỹ năng không được để trống")
    private Long skillId;

    @NotNull(message = "Mức thành thạo không được để trống")
    @Min(value = 1, message = "Mức thành thạo tối thiểu là 1")
    @Max(value = 5, message = "Mức thành thạo tối đa là 5")
    private Integer proficiencyLevel;

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    @DecimalMin(value = "0.0", message = "Số năm kinh nghiệm không được nhỏ hơn 0")
    private BigDecimal yearsOfExperience;

    public DeclareSkillRequest() {
    }

    public DeclareSkillRequest(Long skillId, Integer proficiencyLevel, BigDecimal yearsOfExperience) {
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsOfExperience = yearsOfExperience;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public Integer getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(Integer proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public BigDecimal getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(BigDecimal yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }
}
