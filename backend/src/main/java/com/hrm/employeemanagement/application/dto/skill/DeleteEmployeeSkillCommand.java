package com.hrm.employeemanagement.application.dto.skill;

public record DeleteEmployeeSkillCommand(
        Long employeeId,
        Long skillId
) {
    public DeleteEmployeeSkillCommand {
        if (skillId == null) {
            throw new IllegalArgumentException("ID kỹ năng không được để trống.");
        }
    }
}
