package com.hrm.employeemanagement.application.dto.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;

public record EmployeeSkillResult(
        Long id,
        Long employeeId,
        Long skillId,
        String skillName,
        String skillCode,
        String skillCategory,
        Integer proficiencyLevel,
        BigDecimal yearsOfExperience,
        String status,
        Long approvedBy,
        LocalDateTime approvedAt,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
        ) {

    public static EmployeeSkillResult fromDomain(EmployeeSkill employeeSkill, Skill skill) {
        return new EmployeeSkillResult(
                employeeSkill.getId(),
                employeeSkill.getEmployeeId(),
                employeeSkill.getSkillId(),
                skill != null ? skill.getName() : null,
                skill != null ? skill.getCode() : null,
                skill != null ? skill.getCategory() : null,
                employeeSkill.getProficiencyLevel(),
                employeeSkill.getYearsOfExperience(),
                employeeSkill.getStatus().name(),
                employeeSkill.getApprovedBy(),
                employeeSkill.getApprovedAt(),
                employeeSkill.getRejectionReason(),
                employeeSkill.getCreatedAt(),
                employeeSkill.getUpdatedAt()
        );
    }
}
