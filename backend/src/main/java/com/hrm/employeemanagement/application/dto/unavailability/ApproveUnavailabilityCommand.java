package com.hrm.employeemanagement.application.dto.unavailability;

public record ApproveUnavailabilityCommand(
        Long declarationId,
        String approverComment,
        boolean confirmConflictWarning
) {}
