package com.hrm.employeemanagement.application.dto.task;

public record TaskBoardAssigneeResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        boolean isPrimary
) {
}
