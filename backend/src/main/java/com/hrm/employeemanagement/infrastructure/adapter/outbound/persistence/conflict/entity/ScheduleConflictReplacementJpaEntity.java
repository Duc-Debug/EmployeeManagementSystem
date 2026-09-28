package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@jakarta.persistence.Table(name = "schedule_conflict_replacements")
public class ScheduleConflictReplacementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conflict_id", nullable = false)
    private Long conflictId;

    @Column(name = "original_employee_id", nullable = false)
    private Long originalEmployeeId;

    @Column(name = "replacement_employee_id", nullable = false)
    private Long replacementEmployeeId;

    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "proficiency_level")
    private Integer proficiencyLevel;

    @Column(name = "free_hours", nullable = false)
    private BigDecimal freeHours;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ScheduleConflictReplacementJpaEntity() {
    }

    public ScheduleConflictReplacementJpaEntity(
            Long id,
            Long conflictId,
            Long originalEmployeeId,
            Long replacementEmployeeId,
            Long skillId,
            Integer proficiencyLevel,
            BigDecimal freeHours,
            String status,
            String notes,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.conflictId = conflictId;
        this.originalEmployeeId = originalEmployeeId;
        this.replacementEmployeeId = replacementEmployeeId;
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
        this.freeHours = freeHours;
        this.status = status;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConflictId() {
        return conflictId;
    }

    public void setConflictId(Long conflictId) {
        this.conflictId = conflictId;
    }

    public Long getOriginalEmployeeId() {
        return originalEmployeeId;
    }

    public void setOriginalEmployeeId(Long originalEmployeeId) {
        this.originalEmployeeId = originalEmployeeId;
    }

    public Long getReplacementEmployeeId() {
        return replacementEmployeeId;
    }

    public void setReplacementEmployeeId(Long replacementEmployeeId) {
        this.replacementEmployeeId = replacementEmployeeId;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public Integer getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(Integer proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public BigDecimal getFreeHours() {
        return freeHours;
    }

    public void setFreeHours(BigDecimal freeHours) {
        this.freeHours = freeHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
