package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill;

import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;

public class SkillPersistenceMapper {

    public static Skill toDomain(SkillJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Skill(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public static SkillJpaEntity toJpaEntity(Skill domain) {
        if (domain == null) {
            return null;
        }
        return new SkillJpaEntity(
                domain.getId(),
                domain.getCode(),
                domain.getName(),
                domain.getCategory(),
                domain.getDescription(),
                domain.getCreatedAt()
        );
    }

    public static EmployeeSkill toDomain(EmployeeSkillJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new EmployeeSkill(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getSkillId(),
                entity.getProficiencyLevel(),
                entity.getYearsOfExperience(),
                entity.getStatus(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static EmployeeSkillJpaEntity toJpaEntity(EmployeeSkill domain) {
        if (domain == null) {
            return null;
        }
        return new EmployeeSkillJpaEntity(
                domain.getId(),
                domain.getEmployeeId(),
                domain.getSkillId(),
                domain.getProficiencyLevelValue(),
                domain.getYearsOfExperience(),
                domain.getStatus(),
                domain.getApprovedBy(),
                domain.getApprovedAt(),
                domain.getRejectionReason(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
