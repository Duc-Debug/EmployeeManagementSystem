package com.hrm.employeemanagement.application.dto.scenario.recruitment;

import java.math.BigDecimal;

public record AddSimulatedEmployeeCommand(
        Long scenarioId,
        String candidateName,
        Long projectRoleId,
        Long primarySkillId,
        BigDecimal standardHoursPerWeek,
        Integer weeksCount,
        String notes
) {
}
