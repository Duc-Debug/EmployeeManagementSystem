package com.hrm.employeemanagement.application.dto.workload;

import java.math.BigDecimal;

public record ProjectWorkloadAllocationResult(
        Long projectId,
        String projectName,
        String projectCode,
        Long projectRoleId,
        String projectRoleName,
        BigDecimal allocatedHours,
        BigDecimal allocationPercentage
) {}
