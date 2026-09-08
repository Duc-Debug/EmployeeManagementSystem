package com.hrm.employeemanagement.application.port.outbound.task;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;

public interface LoadTaskPort {
    Optional<Task> findById(TaskId id);

    List<Task> findAllByProjectId(ProjectId projectId);

    boolean existsByIdAndProjectId(TaskId id, ProjectId projectId);

    boolean hasChildren(TaskId parentId);
}