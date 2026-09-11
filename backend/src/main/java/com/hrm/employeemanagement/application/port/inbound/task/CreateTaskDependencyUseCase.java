package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.dependency.CreateTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyResult;

public interface CreateTaskDependencyUseCase {
    TaskDependencyResult createDependency(CreateTaskDependencyCommand command);
}
