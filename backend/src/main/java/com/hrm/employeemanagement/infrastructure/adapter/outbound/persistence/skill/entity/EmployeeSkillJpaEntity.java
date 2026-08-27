package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_skills")
@IdClass(EmployeeSkillId.class)
public class EmployeeSkillJpaEntity {

    @Id
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Id
    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public EmployeeSkillJpaEntity() {
    }

    public EmployeeSkillJpaEntity(Long employeeId, Long skillId, LocalDateTime createdAt) {
        this.employeeId = employeeId;
        this.skillId = skillId;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
