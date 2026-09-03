package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.mapper;

import com.hrm.employeemanagement.domain.skill.*;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillGroupJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;

public class SkillPersistenceMapper {

    public static Skill toDomain(SkillJpaEntity entity) {
        if (entity == null) return null;
        return new Skill(
                entity.getId() != null ? new SkillId(entity.getId()) : null,
                entity.getGroupId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus() != null ? SkillStatus.valueOf(entity.getStatus()) : SkillStatus.ACTIVE,
                entity.getMergedIntoSkillId() != null ? new SkillId(entity.getMergedIntoSkillId()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static SkillJpaEntity toJpaEntity(Skill domain) {
        if (domain == null) return null;
        return new SkillJpaEntity(
                domain.getId() != null ? domain.getId().value() : null,
                domain.getGroupId(),
                domain.getName(),
                domain.getDescription(),
                domain.getStatus() != null ? domain.getStatus().name() : "ACTIVE",
                domain.getMergedIntoSkillId() != null ? domain.getMergedIntoSkillId().value() : null,
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static SkillGroup toDomain(SkillGroupJpaEntity entity) {
        if (entity == null) return null;
        return new SkillGroup(
                entity.getId() != null ? new SkillGroupId(entity.getId()) : null,
                entity.getName(),
                entity.getDescription(),
                entity.getStatus() != null ? SkillStatus.valueOf(entity.getStatus()) : SkillStatus.ACTIVE,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static SkillGroupJpaEntity toJpaEntity(SkillGroup domain) {
        if (domain == null) return null;
        return new SkillGroupJpaEntity(
                domain.getId() != null ? domain.getId().value() : null,
                domain.getName(),
                domain.getDescription(),
                domain.getStatus() != null ? domain.getStatus().name() : "ACTIVE",
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
