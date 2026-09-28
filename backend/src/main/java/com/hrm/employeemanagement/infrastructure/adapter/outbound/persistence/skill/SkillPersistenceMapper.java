package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill;

import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;

public class SkillPersistenceMapper {

    public static Skill toDomain(SkillJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        com.hrm.employeemanagement.domain.skill.SkillStatus status = com.hrm.employeemanagement.domain.skill.SkillStatus.ACTIVE;
        if (entity.getStatus() != null && !entity.getStatus().isBlank()) {
            try {
                status = com.hrm.employeemanagement.domain.skill.SkillStatus.valueOf(entity.getStatus().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                status = com.hrm.employeemanagement.domain.skill.SkillStatus.ACTIVE;
            }
        }
        return new Skill(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getGroupId(),
                entity.getCreatedAt(),
                status
        );
    }

    public static SkillJpaEntity toJpaEntity(Skill domain) {
        if (domain == null) {
            return null;
        }
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

    public static EmployeeSkill toDomain(EmployeeSkillJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new EmployeeSkill(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getSkillId(),
                entity.getProficiencyLevel() != null ? ProficiencyLevel.fromValue(entity.getProficiencyLevel()) : null,
                entity.getYearsOfExperience(),
                entity.getStatus(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason(),
                entity.getReviewNotes(),
                entity.getLastApprovedProficiencyLevel(),
                entity.getLastApprovedYearsOfExperience(),
                entity.getPendingProficiencyLevel(),
                entity.getPendingYearsOfExperience(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
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
                domain.getReviewNotes(),
                domain.getLastApprovedProficiencyLevel(),
                domain.getLastApprovedYearsOfExperience(),
                domain.getPendingProficiencyLevel(),
                domain.getPendingYearsOfExperience(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }
}
