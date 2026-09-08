package com.hrm.employeemanagement.domain.projecttemplate;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.user.UserId;

/**
 * ProjectTemplate: Entity mẫu dự án
 */
public class ProjectTemplate {
    private final ProjectTemplateId id;
    private final String templateCode;
    private final String name;
    private final String description;
    private final boolean active;
    private final UserId createdBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long version;

    public ProjectTemplate(
            ProjectTemplateId id,
            String templateCode,
            String name,
            String description,
            boolean active,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.id = Objects.requireNonNull(id, "ProjectTemplate id must not be null");
        this.templateCode = Objects.requireNonNull(templateCode, "Template code must not be null");
        this.name = Objects.requireNonNull(name, "Template name must not be null");
        this.description = description;
        this.active = active;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public ProjectTemplateId getId() {
        return id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public UserId getCreatedBy() {
        return createdBy;
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
}
