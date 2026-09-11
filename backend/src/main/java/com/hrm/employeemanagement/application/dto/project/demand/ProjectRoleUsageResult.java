package com.hrm.employeemanagement.application.dto.project.demand;

public record ProjectRoleUsageResult(
        Long roleId,
        boolean inUse,
        long demandCount,
        long employeeCount,
        String warningMessage
) {}
