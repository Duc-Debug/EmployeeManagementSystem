package com.hrm.employeemanagement.application.dto.projecttemplate;

import java.math.BigDecimal;

public record ProjectTemplateSummaryResult(
        Long id,
        String templateCode,
        String name,
        String description,
        boolean active,
        BigDecimal totalEstimatedHours,
        int categoriesCount,
        int tasksCount) {
}
