package com.hrm.employeemanagement.domain.project;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.user.UserId;

public class Project {
    private ProjectId id;
    private String projectCode;
    private String projectName;
    private Long orgUnitId;
    private EmployeeId managerId;
    private ProjectStatus status;
    private UserId createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public Project(
            ProjectId id,
            String projectCode,
            String projectName,
            Long orgUnitId,
            EmployeeId managerId,
            ProjectStatus status,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.projectCode = Objects.requireNonNull(
                projectCode,
                "ProjectCode khong duoc null"
        );
        this.projectName = Objects.requireNonNull(
                projectName,
                "ProjectName khong duoc null"
        );
        this.orgUnitId = Objects.requireNonNull(
                orgUnitId,
                "OrgUnitId khong duoc null"
        );
        this.managerId = managerId;
        this.status = status != null
                ? status
                : ProjectStatus.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public ProjectId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
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

    public EmployeeId getManagerId() {
        return managerId;
    }

    public Long getManagerIdValue() {
        return managerId != null ? managerId.value() : null;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public UserId getCreatedBy() {
        return createdBy;
    }

    public Long getCreatedByValue() {
        return createdBy != null ? createdBy.value() : null;
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
