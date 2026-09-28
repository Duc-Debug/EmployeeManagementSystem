package com.hrm.employeemanagement.application.dto.project.demand;
import java.math.BigDecimal;
import java.util.List;
public record ProjectResourceDemandSummaryResult(
        Long projectId,
        String projectCode,
        String projectName,
        BigDecimal projectEstimatedHours,
        BigDecimal totalDemandHours,
        boolean exceedsEstimatedHours,
        String warningMessage,
        List<RoleResourceDemandResult> demandsByRole) {
}