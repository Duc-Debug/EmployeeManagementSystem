package com.hrm.employeemanagement.application.dto.allocation.template;

import java.math.BigDecimal;
import java.util.List;

public record CreateRoleAllocationTemplateCommand(
        String templateCode,
        String name,
        String description,
        Long sourceProjectId,
        List<ItemCommand> items
) {
    public record ItemCommand(
            Long roleId,
            BigDecimal hoursPerWeek
    ) {}
}

