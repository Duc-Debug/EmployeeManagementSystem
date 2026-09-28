package com.hrm.employeemanagement.application.dto.allocation.template;

import java.time.LocalDateTime;

public record RoleAllocationTemplateSummaryResult(
        Long id,
        String templateCode,
        String name,
        String description,
        Long sourceProjectId,
        int itemsCount,
        Long createdBy,
        LocalDateTime createdAt
) {}

