package com.hrm.employeemanagement.application.dto.unavailability;

public record RejectUnavailabilityCommand(
        Long declarationId,
        String rejectReason
) {}
