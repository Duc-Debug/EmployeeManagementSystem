package com.hrm.employeemanagement.domain.allocation.template;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProjectRoleAllocationTemplate {
    private Long id;
    private String templateCode;
    private String name;
    private String description;
    private Long sourceProjectId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private final List<ProjectRoleAllocationTemplateItem> items = new ArrayList<>();

    public ProjectRoleAllocationTemplate(
            Long id,
            String templateCode,
            String name,
            String description,
            Long sourceProjectId,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version,
            List<ProjectRoleAllocationTemplateItem> items
    ) {
        ProjectRoleAllocationTemplatePolicy.validateTemplate(templateCode, name, items);
        this.id = id;
        this.templateCode = templateCode.trim();
        this.name = name.trim();
        this.description = description;
        this.sourceProjectId = sourceProjectId;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
        if (items != null) {
            this.items.addAll(items);
        }
    }

    public static ProjectRoleAllocationTemplate create(
            String templateCode,
            String name,
            String description,
            Long sourceProjectId,
            Long createdBy,
            List<ProjectRoleAllocationTemplateItem> items
    ) {
        return new ProjectRoleAllocationTemplate(
                null,
                templateCode,
                name,
                description,
                sourceProjectId,
                createdBy,
                LocalDateTime.now(),
                null,
                0L,
                items
        );
    }

    public void updateInfo(String name, String description, List<ProjectRoleAllocationTemplateItem> newItems) {
        ProjectRoleAllocationTemplatePolicy.validateName(name);
        ProjectRoleAllocationTemplatePolicy.validateItems(newItems);
        this.name = name.trim();
        this.description = description;
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getSourceProjectId() {
        return sourceProjectId;
    }

    public Long getCreatedBy() {
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

    public List<ProjectRoleAllocationTemplateItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}

