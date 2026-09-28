package com.hrm.employeemanagement.domain.skill;

public record SkillGroupId(Long value) {
    public SkillGroupId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("SkillGroupId phải là số dương hợp lệ.");
        }
    }
}
