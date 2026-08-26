package com.hrm.employeemanagement.application.dto.skill;

import java.util.List;
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
        if (sourceSkillIds.contains(targetSkillId)) {
            throw new InvalidSkillMergeException("Danh sách kỹ năng nguồn không được chứa chính kỹ năng đích.");
        }
        // Loại bỏ null và duplicate
        sourceSkillIds = sourceSkillIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (sourceSkillIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách Source Skill IDs không hợp lệ sau khi lọc.");
        }
    }
}