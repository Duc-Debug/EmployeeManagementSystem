package com.hrm.employeemanagement.application.dto.allocation.template;

import java.util.List;

public record ApplyRoleAllocationTemplateResult(
        Long templateId,
        Long targetProjectId,
        int appliedRolesCount,
        int allocatedEmployeesCount,
        int unassignedRolesCount,
        List<String> warnings,
        String message
) {}

