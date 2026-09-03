package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import java.time.LocalDateTime;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;

public record SkillGroupResponse(
        Long id,
        String name,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SkillGroupResponse fromResult(SkillGroupResult result) {
        if (result == null) return null;
        return new SkillGroupResponse(
                result.id(),
                result.name(),
                result.description(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
