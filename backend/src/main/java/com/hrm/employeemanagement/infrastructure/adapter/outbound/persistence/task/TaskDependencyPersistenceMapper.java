package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependencyType;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskDependencyJpaEntity;

public final class TaskDependencyPersistenceMapper {

    private TaskDependencyPersistenceMapper() {
    }

    public static TaskDependency toDomain(TaskDependencyJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TaskDependency(
                entity.getId(),
                new ProjectId(entity.getProjectId()),
                new TaskId(entity.getPredecessorId()),
                new TaskId(entity.getSuccessorId()),
                TaskDependencyType.valueOf(entity.getDependencyType()),
                entity.getLagDays(),
                entity.getCreatedBy() != null ? new UserId(entity.getCreatedBy()) : null,
                entity.getCreatedAt()
        );
    }

    public static TaskDependencyJpaEntity toJpaEntity(TaskDependency domain) {
        if (domain == null) {
            return null;
        }
        return new TaskDependencyJpaEntity(
                domain.getId(),
                domain.getProjectIdValue(),
                domain.getPredecessorIdValue(),
                domain.getSuccessorIdValue(),
                domain.getDependencyType().name(),
                domain.getLagDays(),
                domain.getCreatedByValue(),
                domain.getCreatedAt()
        );
    }
}
