package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import java.time.LocalDateTime;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;

public record SkillResponse(
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
    public static SkillResponse fromResult(SkillResult result) {
        if (result == null) return null;
        return new SkillResponse(
                result.id(),
                result.groupId(),
                result.groupName(),
                result.name(),
                result.description(),
                result.status(),
                result.mergedIntoSkillId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
