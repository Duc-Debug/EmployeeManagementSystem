package com.hrm.employeemanagement.application.dto.skill;

import java.math.BigDecimal;

public record UpdateEmployeeSkillCommand(
        Long employeeId,
        Long skillId,
        int proficiencyLevel,
        BigDecimal yearsOfExperience
) {
    public UpdateEmployeeSkillCommand {
        if (skillId == null) {
            throw new IllegalArgumentException("ID kỹ năng không được để trống.");
        }
        if (proficiencyLevel < 1 || proficiencyLevel > 5) {
            throw new IllegalArgumentException("Mức thành thạo phải từ 1 đến 5.");
        }
        if (yearsOfExperience == null || yearsOfExperience.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số năm kinh nghiệm không được nhỏ hơn 0.");
        }
    }
}
