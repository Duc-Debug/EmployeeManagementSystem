package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "scenario_simulated_employees")
public class ScenarioSimulatedEmployeeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "candidate_name", nullable = false, length = 150)
    private String candidateName;

    @Column(name = "project_role_id", nullable = false)
    private Long projectRoleId;

    @Column(name = "primary_skill_id")
    private Long primarySkillId;

    @Column(name = "standard_hours_per_week", nullable = false, precision = 5, scale = 2)
    private BigDecimal standardHoursPerWeek = new BigDecimal("40.00");

    @Column(name = "weeks_count", nullable = false)
    private int weeksCount = 4;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public ScenarioSimulatedEmployeeJpaEntity() {
    }

    public ScenarioSimulatedEmployeeJpaEntity(
            Long id,
            Long scenarioId,
            String candidateName,
            Long projectRoleId,
            Long primarySkillId,
            BigDecimal standardHoursPerWeek,
            int weeksCount,
            String notes,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.candidateName = candidateName;
        this.projectRoleId = projectRoleId;
        this.primarySkillId = primarySkillId;
        this.standardHoursPerWeek = standardHoursPerWeek;
        this.weeksCount = weeksCount;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    public void setProjectRoleId(Long projectRoleId) {
        this.projectRoleId = projectRoleId;
    }

    public Long getPrimarySkillId() {
        return primarySkillId;
    }

    public void setPrimarySkillId(Long primarySkillId) {
        this.primarySkillId = primarySkillId;
    }

    public BigDecimal getStandardHoursPerWeek() {
        return standardHoursPerWeek;
    }

    public void setStandardHoursPerWeek(BigDecimal standardHoursPerWeek) {
        this.standardHoursPerWeek = standardHoursPerWeek;
    }

    public int getWeeksCount() {
        return weeksCount;
    }

    public void setWeeksCount(int weeksCount) {
        this.weeksCount = weeksCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
