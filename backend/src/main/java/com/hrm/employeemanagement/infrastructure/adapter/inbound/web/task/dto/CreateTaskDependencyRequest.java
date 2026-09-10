package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import jakarta.validation.constraints.NotNull;

public record CreateTaskDependencyRequest(
        @NotNull(message = "Công việc tiền đề không được để trống")
        Long predecessorId,

        @NotNull(message = "Công việc phụ thuộc không được để trống")
        Long successorId,

        String dependencyType,

        Integer lagDays
) {
}
