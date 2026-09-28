package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.mapper;

import com.hrm.employeemanagement.domain.skill.*;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillGroupJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;

public class SkillPersistenceMapper {

    public static Skill toDomain(SkillJpaEntity entity) {
        if (entity == null) return null;
        return new Skill(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getGroupId(),
                entity.getCreatedAt(),
                entity.getStatus() == null ? null : com.hrm.employeemanagement.domain.skill.SkillStatus.valueOf(entity.getStatus())
        );
    }

    public static SkillJpaEntity toJpaEntity(Skill domain) {
        if (domain == null) return null;
        SkillJpaEntity entity = new SkillJpaEntity(
                domain.getId(),
                domain.getCode(),
                domain.getName(),
                domain.getCategory(),
                domain.getDescription(),
                domain.getCreatedAt()
        );
        entity.setGroupId(domain.getGroupId());
        entity.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        return entity;
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
