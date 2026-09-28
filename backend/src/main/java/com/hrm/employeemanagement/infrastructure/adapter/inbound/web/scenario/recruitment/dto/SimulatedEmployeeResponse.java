package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SimulatedEmployeeResponse(
        Long id,
        Long scenarioId,
        String candidateName,
        Long projectRoleId,
        String projectRoleCode,
        String projectRoleName,
        Long primarySkillId,
        String primarySkillName,
        BigDecimal standardHoursPerWeek,
        int weeksCount,
        BigDecimal totalSimulatedCapacityHours,
        String notes,
        Long createdBy,
        LocalDateTime createdAt
) {
}
