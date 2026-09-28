package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "project_resource_demands")
public class ProjectResourceDemandJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "year_number", nullable = false)
    private Integer year;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "required_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal requiredHours;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public ProjectResourceDemandJpaEntity() {
    }

    public ProjectResourceDemandJpaEntity(
            Long id,
            Long projectId,
            Long roleId,
            Integer year,
            Integer weekNumber,
            BigDecimal requiredHours,
            Long version) {
        this.id = id;
        this.projectId = projectId;
        this.roleId = roleId;
        this.year = year;
        this.weekNumber = weekNumber;
        this.requiredHours = requiredHours;
        this.version = version != null ? version : 0L;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public BigDecimal getRequiredHours() {
        return requiredHours;
    }

    public void setRequiredHours(BigDecimal requiredHours) {
        this.requiredHours = requiredHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}