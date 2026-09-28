package com.hrm.employeemanagement.application.port.outbound.task;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;

public interface LoadTaskDependencyPort {
    List<TaskDependency> findByProjectId(ProjectId projectId);
    List<TaskDependency> findByProjectIdAndPredecessorId(ProjectId projectId, TaskId predecessorId);
    List<TaskDependency> findByProjectIdAndSuccessorId(ProjectId projectId, TaskId successorId);
    Optional<TaskDependency> findById(Long id);
    boolean existsByPredecessorIdAndSuccessorId(TaskId predecessorId, TaskId successorId);
}
