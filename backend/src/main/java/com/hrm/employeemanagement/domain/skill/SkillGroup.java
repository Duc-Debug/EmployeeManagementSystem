package com.hrm.employeemanagement.domain.skill;

import java.time.LocalDateTime;
import com.hrm.employeemanagement.domain.exception.skill.RequiredFieldMissingException;

public class SkillGroup {
    private final SkillGroupId id;
    private String name;
    private String description;
    private SkillStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SkillGroup(SkillGroupId id, String name, String description,
                      SkillStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (name == null || name.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên nhóm kỹ năng (name)");
        }
        this.id = id;
        this.name = name.trim();
        this.description = description;
        this.status = status != null ? status : SkillStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
    }

    public void updateInfo(String newName, String newDescription) {
        if (newName == null || newName.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên nhóm kỹ năng (name)");
        }
        this.name = newName.trim();
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = SkillStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public SkillGroupId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SkillStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
