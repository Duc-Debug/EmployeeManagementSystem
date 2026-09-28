package com.hrm.employeemanagement.application.dto.skill;

import com.hrm.employeemanagement.domain.exception.skill.RequiredFieldMissingException;

public record CreateSkillCommand(
        Long groupId,
        String name,
        String description
) {
    public CreateSkillCommand {
        if (groupId == null || groupId <= 0) {
            throw RequiredFieldMissingException.of("Nhóm kỹ năng (groupId)");
        }
        if (name == null || name.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên kỹ năng (name)");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Tên kỹ năng không được vượt quá 100 ký tự.");
        }
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("Mô tả kỹ năng không được vượt quá 1000 ký tự.");
        }
        name = name.trim();
        description = description != null ? description.trim() : null;
    }
}
