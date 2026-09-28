package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.skill.SkillStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "employee_skills", uniqueConstraints = {
    @UniqueConstraint(name = "uq_employee_skill", columnNames = {"employee_id", "skill_id"})
})
public class EmployeeSkillJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "proficiency_level", nullable = false)
    private Integer proficiencyLevel;

    @Column(name = "years_of_experience", nullable = false, precision = 4, scale = 1)
    private BigDecimal yearsOfExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SkillStatus status;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @Column(name = "last_approved_proficiency_level")
    private Integer lastApprovedProficiencyLevel;

    @Column(name = "last_approved_years_of_experience", precision = 4, scale = 1)
    private BigDecimal lastApprovedYearsOfExperience;

    @Column(name = "pending_proficiency_level")
    private Integer pendingProficiencyLevel;

    @Column(name = "pending_years_of_experience", precision = 4, scale = 1)
    private BigDecimal pendingYearsOfExperience;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public EmployeeSkillJpaEntity() {
    }

    public EmployeeSkillJpaEntity(
            Long id, Long employeeId, Long skillId, Integer proficiencyLevel, BigDecimal yearsOfExperience,
            SkillStatus status, Long approvedBy, LocalDateTime approvedAt, String rejectionReason,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this(id, employeeId, skillId, proficiencyLevel, yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, null, null, null, null, null, createdAt, updatedAt, null);
    }

    public EmployeeSkillJpaEntity(
            Long id, Long employeeId, Long skillId, Integer proficiencyLevel, BigDecimal yearsOfExperience,
            SkillStatus status, Long approvedBy, LocalDateTime approvedAt, String rejectionReason,
            String reviewNotes, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this(id, employeeId, skillId, proficiencyLevel, yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, reviewNotes, null, null, null, null, createdAt, updatedAt, null);
    }

    public EmployeeSkillJpaEntity(
            Long id, Long employeeId, Long skillId, Integer proficiencyLevel, BigDecimal yearsOfExperience,
            SkillStatus status, Long approvedBy, LocalDateTime approvedAt, String rejectionReason,
            String reviewNotes, LocalDateTime createdAt, LocalDateTime updatedAt, Long version
    ) {
        this(id, employeeId, skillId, proficiencyLevel, yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, reviewNotes, null, null, null, null, createdAt, updatedAt, version);
    }

    public EmployeeSkillJpaEntity(
            Long id, Long employeeId, Long skillId, Integer proficiencyLevel, BigDecimal yearsOfExperience,
            SkillStatus status, Long approvedBy, LocalDateTime approvedAt, String rejectionReason,
            String reviewNotes, Integer lastApprovedProficiencyLevel, BigDecimal lastApprovedYearsOfExperience,
            LocalDateTime createdAt, LocalDateTime updatedAt, Long version
    ) {
        this(id, employeeId, skillId, proficiencyLevel, yearsOfExperience, status, approvedBy, approvedAt, rejectionReason, reviewNotes, lastApprovedProficiencyLevel, lastApprovedYearsOfExperience, null, null, createdAt, updatedAt, version);
    }

    public EmployeeSkillJpaEntity(
            Long id, Long employeeId, Long skillId, Integer proficiencyLevel, BigDecimal yearsOfExperience,
            SkillStatus status, Long approvedBy, LocalDateTime approvedAt, String rejectionReason,
            String reviewNotes, Integer lastApprovedProficiencyLevel, BigDecimal lastApprovedYearsOfExperience,
            Integer pendingProficiencyLevel, BigDecimal pendingYearsOfExperience,
            LocalDateTime createdAt, LocalDateTime updatedAt, Long version
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsOfExperience = yearsOfExperience;
        this.status = status;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
        this.reviewNotes = reviewNotes;
        this.lastApprovedProficiencyLevel = lastApprovedProficiencyLevel;
        this.lastApprovedYearsOfExperience = lastApprovedYearsOfExperience;
        this.pendingProficiencyLevel = pendingProficiencyLevel;
        this.pendingYearsOfExperience = pendingYearsOfExperience;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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

    public BigDecimal getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(BigDecimal yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public SkillStatus getStatus() {
        return status;
    }

    public void setStatus(SkillStatus status) {
        this.status = status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Integer getLastApprovedProficiencyLevel() {
        return lastApprovedProficiencyLevel;
    }

    public void setLastApprovedProficiencyLevel(Integer lastApprovedProficiencyLevel) {
        this.lastApprovedProficiencyLevel = lastApprovedProficiencyLevel;
    }

    public BigDecimal getLastApprovedYearsOfExperience() {
        return lastApprovedYearsOfExperience;
    }

    public void setLastApprovedYearsOfExperience(BigDecimal lastApprovedYearsOfExperience) {
        this.lastApprovedYearsOfExperience = lastApprovedYearsOfExperience;
    }

    public Integer getPendingProficiencyLevel() {
        return pendingProficiencyLevel;
    }

    public void setPendingProficiencyLevel(Integer pendingProficiencyLevel) {
        this.pendingProficiencyLevel = pendingProficiencyLevel;
    }

    public BigDecimal getPendingYearsOfExperience() {
        return pendingYearsOfExperience;
    }

    public void setPendingYearsOfExperience(BigDecimal pendingYearsOfExperience) {
        this.pendingYearsOfExperience = pendingYearsOfExperience;
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
