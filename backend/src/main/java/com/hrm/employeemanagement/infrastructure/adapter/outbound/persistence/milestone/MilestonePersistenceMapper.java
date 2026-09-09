package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.milestone;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneId;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.milestone.entity.ProjectMilestoneJpaEntity;

public final class MilestonePersistenceMapper {

    private MilestonePersistenceMapper() {
    }

    public static Milestone toDomain(ProjectMilestoneJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Set<TaskId> taskIds = new HashSet<>();
        if (entity.getLinkedTaskIds() != null) {
            taskIds = entity.getLinkedTaskIds().stream()
                    .map(TaskId::new)
                    .collect(Collectors.toSet());
        }

        return new Milestone(
                new MilestoneId(entity.getId()),
                new ProjectId(entity.getProjectId()),
                entity.getName(),
                entity.getDescription(),
                entity.getPlannedDate(),
                entity.getActualDate(),
                taskIds,
                entity.getCreatedBy() != null ? new UserId(entity.getCreatedBy()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    public static ProjectMilestoneJpaEntity toEntity(Milestone domain) {
        if (domain == null) {
            return null;
        }

        Set<Long> taskIds = new HashSet<>();
        if (domain.getLinkedTaskIds() != null) {
            taskIds = domain.getLinkedTaskIds().stream()
                    .map(TaskId::value)
                    .collect(Collectors.toSet());
        }

        return new ProjectMilestoneJpaEntity(
                domain.getIdValue(),
                domain.getProjectIdValue(),
                domain.getName(),
                domain.getDescription(),
                domain.getPlannedDate(),
                domain.getActualDate(),
                taskIds,
                domain.getCreatedByValue(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion() != null ? domain.getVersion() : 0L);
    }
}
