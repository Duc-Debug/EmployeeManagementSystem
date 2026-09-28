package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;

@Component
public class ProjectResourceDemandPersistenceMapper {

    public ProjectResourceDemand toDomain(ProjectResourceDemandJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        ProjectId projectId = entity.getProjectId() != null ? new ProjectId(entity.getProjectId()) : null;
        ProjectRoleId roleId = entity.getRoleId() != null ? new ProjectRoleId(entity.getRoleId()) : null;
        YearWeek yearWeek = YearWeek.of(entity.getYear(), entity.getWeekNumber());

        return new ProjectResourceDemand(
                entity.getId(),
                projectId,
                roleId,
                yearWeek,
                entity.getRequiredHours(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    public ProjectResourceDemandJpaEntity toJpaEntity(ProjectResourceDemand domain) {
        if (domain == null) {
            return null;
        }

        return new ProjectResourceDemandJpaEntity(
                domain.getId(),
                domain.getProjectIdValue(),
                domain.getRoleIdValue(),
                domain.getYear(),
                domain.getWeekNumber(),
                domain.getRequiredHours(),
                domain.getVersion());
    }
}