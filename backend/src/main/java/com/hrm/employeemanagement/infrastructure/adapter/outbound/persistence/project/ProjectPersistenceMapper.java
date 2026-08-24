package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;

@Component
public class ProjectPersistenceMapper {

    public Project toDomain(ProjectJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        ProjectId projectId = entity.getId() != null
                ? new ProjectId(entity.getId())
                : null;

        EmployeeId managerId = entity.getManagerId() != null
                ? new EmployeeId(entity.getManagerId())
                : null;

        UserId createdBy = entity.getCreatedBy() != null
                ? new UserId(entity.getCreatedBy())
                : null;

        return new Project(
                projectId,
                entity.getProjectCode(),
                entity.getProjectName(),
                entity.getOrgUnitId(),
                managerId,
                entity.getStatus(),
                createdBy,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public ProjectJpaEntity toJpaEntity(Project domain) {
        if (domain == null) {
            return null;
        }

        return new ProjectJpaEntity(
                domain.getIdValue(),
                domain.getProjectCode(),
                domain.getProjectName(),
                domain.getOrgUnitId(),
                domain.getManagerIdValue(),
                domain.getStatus(),
                domain.getCreatedByValue(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }
}
