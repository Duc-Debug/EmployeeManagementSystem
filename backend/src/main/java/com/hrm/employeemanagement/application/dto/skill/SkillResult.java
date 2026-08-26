package com.hrm.employeemanagement.application.dto.skill;

import java.time.LocalDateTime;

public record SkillResult(
        Long id,
        Long groupId,
        String groupName,
        String name,
        String description,
        String status,
        Long mergedIntoSkillId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
