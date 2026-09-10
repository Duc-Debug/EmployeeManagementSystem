package com.hrm.employeemanagement.domain.project.demand;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.project.ProjectId;

public class ProjectResourceDemand {

    private Long id;
    private final ProjectId projectId;
    private final ProjectRoleId roleId;
    private final YearWeek yearWeek;
    private BigDecimal requiredHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public ProjectResourceDemand(
            Long id,
            ProjectId projectId,
            ProjectRoleId roleId,
            YearWeek yearWeek,
            BigDecimal requiredHours,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId không được null");
        this.roleId = Objects.requireNonNull(roleId, "RoleId không được null");
        this.yearWeek = Objects.requireNonNull(yearWeek, "YearWeek không được null");
        ProjectResourceDemandPolicy.validateRequiredHours(requiredHours);
        this.id = id;
        this.requiredHours = requiredHours;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static ProjectResourceDemand createNew(
            ProjectId projectId,
            ProjectRoleId roleId,
            YearWeek yearWeek,
            BigDecimal requiredHours) {
        return new ProjectResourceDemand(
                null,
                projectId,
                roleId,
                yearWeek,
                requiredHours,
                LocalDateTime.now(),
                null,
                0L);
    }

    /**
     * Cập nhật số giờ nhu cầu cho tuần này (dùng khi PM ước lượng lại).
     */
    public void updateRequiredHours(BigDecimal newHours) {
        ProjectResourceDemandPolicy.validateRequiredHours(newHours);
        this.requiredHours = newHours;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== GETTERS ====================
    public Long getId() {
        return id;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public Long getProjectIdValue() {
        return projectId != null ? projectId.value() : null;
    }

    public ProjectRoleId getRoleId() {
        return roleId;
    }

    public Long getRoleIdValue() {
        return roleId != null ? roleId.value() : null;
    }

    public YearWeek getYearWeek() {
        return yearWeek;
    }

    public int getYear() {
        return yearWeek.year();
    }

    public int getWeekNumber() {
        return yearWeek.weekNumber();
    }

    public BigDecimal getRequiredHours() {
        return requiredHours;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectResourceDemand that = (ProjectResourceDemand) o;
        return Objects.equals(projectId, that.projectId)
                && Objects.equals(roleId, that.roleId)
                && Objects.equals(yearWeek, that.yearWeek);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, roleId, yearWeek);
    }
}