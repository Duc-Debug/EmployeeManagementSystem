package com.hrm.employeemanagement.application.dto.allocation.template;

import java.time.LocalDateTime;
import java.util.List;

public record RoleAllocationTemplateDetailResult(
        Long id,
        String templateCode,
        String name,
        String description,
        Long sourceProjectId,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version,
        List<RoleAllocationTemplateItemResult> items
) {}

