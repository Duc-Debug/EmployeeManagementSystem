package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_role_allocation_template_items")
public class ProjectRoleAllocationTemplateItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProjectRoleAllocationTemplateJpaEntity template;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "hours_per_week", nullable = false, precision = 8, scale = 2)
    private BigDecimal hoursPerWeek;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProjectRoleAllocationTemplateItemJpaEntity() {
    }

    public ProjectRoleAllocationTemplateItemJpaEntity(
            Long id,
            ProjectRoleAllocationTemplateJpaEntity template,
            Long roleId,
            BigDecimal hoursPerWeek,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.template = template;
        this.roleId = roleId;
        this.hoursPerWeek = hoursPerWeek;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectRoleAllocationTemplateJpaEntity getTemplate() {
        return template;
    }

    public void setTemplate(ProjectRoleAllocationTemplateJpaEntity template) {
        this.template = template;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public BigDecimal getHoursPerWeek() {
        return hoursPerWeek;
    }

    public void setHoursPerWeek(BigDecimal hoursPerWeek) {
        this.hoursPerWeek = hoursPerWeek;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

