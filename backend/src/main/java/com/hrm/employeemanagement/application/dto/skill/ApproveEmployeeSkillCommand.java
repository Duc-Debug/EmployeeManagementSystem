package com.hrm.employeemanagement.application.dto.skill;

public record ApproveEmployeeSkillCommand(
        Long employeeSkillId,
        Integer adjustedProficiencyLevel,
        String reviewNotes
) {}
