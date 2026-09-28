package com.hrm.employeemanagement.application.dto.allocation.template;

import java.math.BigDecimal;
import java.util.List;

public record PreviewRoleAllocationResult(
        Long templateId,
        String templateCode,
        String templateName,
        Long targetProjectId,
        String targetProjectName,
        int targetTotalWeeks,
        List<RoleSuggestionItemResult> suggestions,
        boolean hasUnassignedRoles,
        List<String> warnings
) {
    public record RoleSuggestionItemResult(
            Long roleId,
            String roleCode,
            String roleName,
            BigDecimal hoursPerWeek,
            Long suggestedEmployeeId,
            String suggestedEmployeeName,
            String suggestedEmployeeCode,
            boolean assigned,
            String warningMessage
    ) {}
}

