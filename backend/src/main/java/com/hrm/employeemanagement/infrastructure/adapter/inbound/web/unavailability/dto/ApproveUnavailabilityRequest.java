package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto;

public record ApproveUnavailabilityRequest(
        String approverComment,
        Boolean confirmConflictWarning
) {}
