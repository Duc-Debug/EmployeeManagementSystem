package com.hrm.employeemanagement.domain.skill;

public record SkillId(Long value) {
    public SkillId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("SkillId phải là số dương hợp lệ.");
        }
    }
}