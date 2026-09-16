package com.hrm.employeemanagement.application.dto.scenario.recruitment;

import java.math.BigDecimal;

public record UpdateSimulatedEmployeeCommand(
        Long scenarioId,
        Long employeeId,
        String candidateName,
        Long projectRoleId,
        Long primarySkillId,
        BigDecimal standardHoursPerWeek,
        Integer weeksCount,
        String notes
) {
}