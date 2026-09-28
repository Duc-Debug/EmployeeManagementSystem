package com.hrm.employeemanagement.application.dto.skill;

import java.time.LocalDateTime;

public record SkillGroupResult(
        Long id,
        String name,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
