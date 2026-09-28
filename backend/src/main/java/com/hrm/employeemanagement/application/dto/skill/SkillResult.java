package com.hrm.employeemanagement.application.dto.skill;

import java.time.LocalDateTime;

public record SkillResult(
        Long id,
        Long groupId,
        String groupName,
        String name,
        String description,
        String status,               // ACTIVE, INACTIVE, MERGED
        Long mergedIntoSkillId,      // Null nếu chưa từng bị merge
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}