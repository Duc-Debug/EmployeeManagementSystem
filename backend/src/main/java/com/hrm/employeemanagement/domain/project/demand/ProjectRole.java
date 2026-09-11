package com.hrm.employeemanagement.domain.project.demand;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleDataException;

public class ProjectRole {

    private final ProjectRoleId id;
    private final String code;
    private String name;
    private String description;
    private Long skillGroupId;
    private String skillGroupName;
    private ProjectRoleStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProjectRole(ProjectRoleId id, String code, String name, String description) {
        this(id, code, name, description, null, null, ProjectRoleStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    public ProjectRole(ProjectRoleId id, String code, String name, String description,
                       Long skillGroupId, ProjectRoleStatus status) {
        this(id, code, name, description, skillGroupId, null, status, LocalDateTime.now(), LocalDateTime.now());
    }

    public ProjectRole(ProjectRoleId id, String code, String name, String description,
                       Long skillGroupId, String skillGroupName, ProjectRoleStatus status,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = validateCode(code);
        this.name = validateName(name);
        this.description = description != null ? description.trim() : null;
        this.skillGroupId = skillGroupId;
        this.skillGroupName = skillGroupName;
        this.status = status != null ? status : ProjectRoleStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static ProjectRole createNew(String code, String name, String description, Long skillGroupId) {
        if (skillGroupId == null) {
            throw new InvalidProjectRoleDataException("Nhóm kỹ năng không được để trống");
        }
        return new ProjectRole(null, code, name, description, skillGroupId, null, ProjectRoleStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public void updateInfo(String newName, Long newSkillGroupId, String newDescription) {
        this.name = validateName(newName);
        if (newSkillGroupId == null) {
            throw new InvalidProjectRoleDataException("Nhóm kỹ năng không được để trống");
        }
        this.skillGroupId = newSkillGroupId;
        this.description = newDescription != null ? newDescription.trim() : null;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = ProjectRoleStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = ProjectRoleStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == ProjectRoleStatus.ACTIVE;
    }

    private static String validateCode(String c) {
        if (c == null || c.trim().isBlank()) {
            throw new InvalidProjectRoleDataException("Mã vai trò chuyên môn không được để trống");
        }
        String trimmed = c.trim().toUpperCase();
        if (trimmed.length() > 50) {
            throw new InvalidProjectRoleDataException("Mã vai trò chuyên môn không được vượt quá 50 ký tự");
        }
        return trimmed;
    }

    private static String validateName(String n) {
        if (n == null || n.trim().isBlank()) {
            throw new InvalidProjectRoleDataException("Tên vai trò chuyên môn không được để trống");
        }
        String trimmed = n.trim();
        if (trimmed.length() > 100) {
            throw new InvalidProjectRoleDataException("Tên vai trò chuyên môn không được vượt quá 100 ký tự");
        }
        return trimmed;
    }

    public ProjectRoleId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getSkillGroupId() {
        return skillGroupId;
    }

    public String getSkillGroupName() {
        return skillGroupName;
    }

    public void setSkillGroupName(String skillGroupName) {
        this.skillGroupName = skillGroupName;
    }

    public ProjectRoleStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
