package com.hrm.employeemanagement.application.dto.task;

public record TaskBoardQuery(
        Long projectId,
        Long employeeId
) {
}
