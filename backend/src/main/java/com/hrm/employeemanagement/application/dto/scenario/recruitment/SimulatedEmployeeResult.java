package com.hrm.employeemanagement.application.dto.scenario.recruitment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SimulatedEmployeeResult(
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
