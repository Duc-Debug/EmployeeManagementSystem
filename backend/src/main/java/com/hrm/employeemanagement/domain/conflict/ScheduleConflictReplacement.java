package com.hrm.employeemanagement.domain.conflict;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ScheduleConflictReplacement {
    private final Long id;
    private final Long conflictId;
    private final Long originalEmployeeId;
    private final Long replacementEmployeeId;
    private final Long skillId;
    private final Integer proficiencyLevel;
    private final BigDecimal freeHours;
    private final String status;
    private final String notes;
    private final Long createdBy;
    private final LocalDateTime createdAt;

    public ScheduleConflictReplacement(
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
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.conflictId = conflictId;
        this.originalEmployeeId = originalEmployeeId;
        this.replacementEmployeeId = replacementEmployeeId;
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
        this.freeHours = freeHours;
        this.status = status != null ? status : "PROPOSED";
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getConflictId() { return conflictId; }
    public Long getOriginalEmployeeId() { return originalEmployeeId; }
    public Long getReplacementEmployeeId() { return replacementEmployeeId; }
    public Long getSkillId() { return skillId; }
    public Integer getProficiencyLevel() { return proficiencyLevel; }
    public BigDecimal getFreeHours() { return freeHours; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
