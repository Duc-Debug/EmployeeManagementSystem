package com.hrm.employeemanagement.application.dto.task.dependency;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;

public record TaskDependencyResult(
        Long id,
        Long projectId,
        Long predecessorId,
        String predecessorTaskCode,
        String predecessorTaskName,
        Long successorId,
        String successorTaskCode,
        String successorTaskName,
        String dependencyType,
        Integer lagDays,
        Long createdBy,
        LocalDateTime createdAt
) {
    public static TaskDependencyResult fromDomain(
            TaskDependency domain,
            String predecessorCode,
            String predecessorName,
            String successorCode,
            String successorName) {
        return new TaskDependencyResult(
                domain.getId(),
                domain.getProjectIdValue(),
                domain.getPredecessorIdValue(),
                predecessorCode,
                predecessorName,
                domain.getSuccessorIdValue(),
                successorCode,
                successorName,
                domain.getDependencyType().name(),
                domain.getLagDays(),
                domain.getCreatedByValue(),
                domain.getCreatedAt()
        );
    }
}
