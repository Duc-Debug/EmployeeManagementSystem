package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyGraphResult;

public interface GetTaskDependenciesUseCase {
    TaskDependencyGraphResult getTaskDependencies(Long projectId);
}
