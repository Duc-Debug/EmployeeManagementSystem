package com.hrm.employeemanagement.application.dto.task.dependency;

public record CreateTaskDependencyCommand(
        Long projectId,
        Long predecessorId,
        Long successorId,
        String dependencyType,
        Integer lagDays
) {
}
