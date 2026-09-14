package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_role_skills")
public class ProjectRoleSkillJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "required_level")
    private Integer requiredLevel;

    @Column(name = "status", nullable = false)
    private String status;

    public Long getId() { return id; }
    public Long getRoleId() { return roleId; }
    public Long getSkillId() { return skillId; }
    public Integer getRequiredLevel() { return requiredLevel; }
    public String getStatus() { return status; }
}
