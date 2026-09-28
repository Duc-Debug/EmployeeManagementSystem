package com.hrm.employeemanagement.application.dto.projecttemplate;

import java.math.BigDecimal;

public record ProjectTemplateTaskResult(
        Long id,
        Long parentId,
        String name,
        String description,
        String taskType,
        BigDecimal estimatedHours,
        int sortOrder) {
}
