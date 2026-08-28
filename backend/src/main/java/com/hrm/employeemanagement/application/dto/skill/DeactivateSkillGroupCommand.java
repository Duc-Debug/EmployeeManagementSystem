package com.hrm.employeemanagement.application.dto.skill;

public record DeactivateSkillGroupCommand(Long id) {
    public DeactivateSkillGroupCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID nhóm kỹ năng phải là số dương hợp lệ.");
        }
    }
}
