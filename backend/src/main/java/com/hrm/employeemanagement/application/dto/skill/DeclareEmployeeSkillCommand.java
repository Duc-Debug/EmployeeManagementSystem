package com.hrm.employeemanagement.application.dto.skill;

public record DeclareEmployeeSkillCommand(
        Long employeeId,
        Long skillId,
        Integer proficiencyLevel,
        Double yearsOfExperience
        ) {

}
