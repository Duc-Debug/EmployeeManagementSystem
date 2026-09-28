package com.hrm.employeemanagement.application.dto.skill;

public record DeactivateSkillCommand(Long id) {
    public DeactivateSkillCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID kỹ năng phải là số dương hợp lệ.");
        }
    }
}
