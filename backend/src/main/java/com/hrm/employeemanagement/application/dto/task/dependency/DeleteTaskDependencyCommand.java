package com.hrm.employeemanagement.application.dto.task.dependency;

public record DeleteTaskDependencyCommand(
        Long projectId,
        Long dependencyId
) {
}
