package com.hrm.employeemanagement.application.dto.skill;

import java.math.BigDecimal;

public record DeclareEmployeeSkillCommand(
        Long employeeId,
        Long skillId,
        Integer proficiencyLevel,
        BigDecimal yearsOfExperience
) {
    public DeclareEmployeeSkillCommand {
        if (employeeId == null) {
            throw new IllegalArgumentException("ID nhân viên không được để trống");
        }
        if (skillId == null) {
            throw new IllegalArgumentException("ID kỹ năng không được để trống");
        }
        if (proficiencyLevel == null) {
            throw new IllegalArgumentException("Mức thành thạo không được để trống");
        }
        if (yearsOfExperience == null) {
            throw new IllegalArgumentException("Số năm kinh nghiệm không được để trống");
        }
    }
}
