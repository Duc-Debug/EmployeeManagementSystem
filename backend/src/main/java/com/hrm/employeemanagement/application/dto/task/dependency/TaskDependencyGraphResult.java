package com.hrm.employeemanagement.application.dto.task.dependency;

import java.util.List;

public record TaskDependencyGraphResult(
        Long projectId,
        String projectCode,
        String projectName,
        List<TaskDependencyResult> dependencies
) {
}
