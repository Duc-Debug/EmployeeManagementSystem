package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.dependency.DeleteTaskDependencyCommand;

public interface DeleteTaskDependencyUseCase {
    void deleteDependency(DeleteTaskDependencyCommand command);
}
