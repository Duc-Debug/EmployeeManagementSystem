package com.hrm.employeemanagement.application.dto.skill;

import java.util.List;
import java.util.Objects;
import com.hrm.employeemanagement.domain.exception.skill.InvalidSkillMergeException;

public record MergeSkillCommand(
        Long targetSkillId,
        List<Long> sourceSkillIds
) {
    public MergeSkillCommand {
        if (targetSkillId == null || targetSkillId <= 0) {
            throw new IllegalArgumentException("Target Skill ID phải là số dương hợp lệ.");
        }
        if (sourceSkillIds == null || sourceSkillIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách Source Skill IDs không được để trống.");
        }
        if (sourceSkillIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Source Skill ID không được null.");
        }
        if (sourceSkillIds.stream().anyMatch(id -> id <= 0)) {
            throw new IllegalArgumentException("Source Skill ID phải là số dương hợp lệ.");
        }
        if (sourceSkillIds.contains(targetSkillId)) {
            throw new InvalidSkillMergeException("Danh sách kỹ năng nguồn không được chứa chính kỹ năng đích.");
        }
        sourceSkillIds = sourceSkillIds.stream()
                .distinct()
                .toList();

        if (sourceSkillIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách Source Skill IDs không được để trống.");
        }
    }
}