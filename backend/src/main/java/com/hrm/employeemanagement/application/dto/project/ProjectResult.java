package com.hrm.employeemanagement.application.dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal estimatedHours;
    private final String description;

    public ProjectResult(
            Long id,
            String projectCode,
            String projectName,
            Long orgUnitId,
            Long managerId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal estimatedHours,
            String description,
            ProjectStatus status,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.orgUnitId = orgUnitId;
        this.managerId = managerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.estimatedHours = estimatedHours;
        this.description = description;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public String getDescription() {
        return description;
    }
}
