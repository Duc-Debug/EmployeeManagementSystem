package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskAssignmentJpaEntity;

@Component
public class TaskAssignmentPersistenceMapper {

    public TaskAssignment toDomain(TaskAssignmentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TaskAssignment(
                entity.getId(),
                new TaskId(entity.getTaskId()),
                new EmployeeId(entity.getEmployeeId()),
                entity.getAssignedAt(),
                entity.getAssignedBy() != null ? new UserId(entity.getAssignedBy()) : null,
                Boolean.TRUE.equals(entity.getPrimary()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public TaskAssignmentJpaEntity toJpaEntity(TaskAssignment domain) {
        if (domain == null) {
            return null;
        }
        return new TaskAssignmentJpaEntity(
                domain.getId(),
                domain.getTaskId() != null ? domain.getTaskId().value() : null,
                domain.getEmployeeId() != null ? domain.getEmployeeId().value() : null,
                domain.getAssignedAt(),
                domain.getAssignedBy() != null ? domain.getAssignedBy().value() : null,
                domain.isPrimary(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }
}

