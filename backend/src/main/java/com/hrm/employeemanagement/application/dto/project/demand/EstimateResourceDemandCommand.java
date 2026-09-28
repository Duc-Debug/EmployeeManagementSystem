package com.hrm.employeemanagement.application.dto.project.demand;

import java.math.BigDecimal;

public record EstimateResourceDemandCommand(
        Long projectId,
        Long roleId,
        BigDecimal hoursPerWeek) {
}