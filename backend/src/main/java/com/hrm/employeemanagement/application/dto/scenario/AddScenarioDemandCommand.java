package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;

public record AddScenarioDemandCommand(
        Long scenarioId,
        String demandName,
        Integer headcount,
        Integer weekStart,
        Integer weekEnd,
        BigDecimal hoursPerWeekPerPerson,
        String skillRequirement
) {
}
