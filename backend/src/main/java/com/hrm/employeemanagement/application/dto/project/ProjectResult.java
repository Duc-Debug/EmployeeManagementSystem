package com.hrm.employeemanagement.application.dto.project;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.project.ProjectStatus;

public class ProjectResult {
    private final Long id;
    private final String projectCode;
    private final String projectName;
    private final Long orgUnitId;
    private final Long managerId;
    private final ProjectStatus status;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProjectResult(
            Long id,
            String projectCode,
            String projectName,
            Long orgUnitId,
            Long managerId,
            ProjectStatus status,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.orgUnitId = orgUnitId;
        this.managerId = managerId;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public ProjectStatus getStatus() {
        return status;
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
}
