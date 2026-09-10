package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;

@Component
public class TaskPersistenceMapper {

    public Task toDomain(TaskJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        TaskId taskId = entity.getId() != null ? new TaskId(entity.getId()) : null;
        ProjectId projectId = entity.getProjectId() != null ? new ProjectId(entity.getProjectId()) : null;
        TaskId parentId = entity.getParentId() != null ? new TaskId(entity.getParentId()) : null;
        EmployeeId assigneeId = entity.getAssigneeId() != null ? new EmployeeId(entity.getAssigneeId()) : null;
        UserId createdBy = entity.getCreatedBy() != null ? new UserId(entity.getCreatedBy()) : null;

        return new Task(
                taskId,
                projectId,
                parentId,
                entity.getTaskCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getTaskType(),
                assigneeId,
                entity.getEstimatedHours(),
                entity.getActualHours(),
                entity.getBudgetHours(),
                entity.getStatus(),
                entity.getSortOrder(),
                createdBy,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    public TaskJpaEntity toJpaEntity(Task domain) {
        if (domain == null) {
            return null;
        }

        return new TaskJpaEntity(
                domain.getIdValue(),
                domain.getProjectIdValue(),
                domain.getParentIdValue(),
                domain.getTaskCode(),
                domain.getName(),
                domain.getDescription(),
                domain.getTaskType(),
                domain.getAssigneeIdValue(),
                domain.getEstimatedHours(),
                domain.getActualHours(),
                domain.getBudgetHours(),
                domain.getStatus(),
                domain.getSortOrder(),
                domain.getCreatedByValue(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion());
    }
}
