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

        UserId closedBy = entity.getClosedBy() != null
                ? new UserId(entity.getClosedBy())
                : null;

        UserId reopenedBy = entity.getReopenedBy() != null
                ? new UserId(entity.getReopenedBy())
                : null;

        return new Project(
                projectId,
                entity.getProjectCode(),
                entity.getProjectName(),
                entity.getOrgUnitId(),
                managerId,
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getEstimatedHours(),
                entity.getDescription(),
                entity.getStatus(),
                createdBy,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                entity.getTaskSeqCounter(),
                entity.getClosureReason(),
                entity.getClosedAt(),
                closedBy,
                entity.getReopenReason(),
                entity.getReopenedAt(),
                reopenedBy);
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
                domain.getStartDate(),
                domain.getEndDate(),
                domain.getEstimatedHours(),
                domain.getDescription(),
                domain.getStatus(),
                domain.getCreatedByValue(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion(),
                domain.getTaskSeqCounter(),
                domain.getClosureReason(),
                domain.getClosedAt(),
                domain.getClosedByValue(),
                domain.getReopenReason(),
                domain.getReopenedAt(),
                domain.getReopenedByValue());
    }
}
