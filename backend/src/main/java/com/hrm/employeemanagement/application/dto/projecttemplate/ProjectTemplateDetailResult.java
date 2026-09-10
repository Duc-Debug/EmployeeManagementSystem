package com.hrm.employeemanagement.application.dto.projecttemplate;

import java.math.BigDecimal;
import java.util.List;

public record ProjectTemplateDetailResult(
        Long id,
        String templateCode,
        String name,
        String description,
        boolean active,
        BigDecimal totalEstimatedHours,
        int categoriesCount,
        int tasksCount,
        List<ProjectTemplateTaskResult> tasks) {
}
