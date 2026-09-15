package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;

public record UpdateScenarioDemandCommand(
        Long scenarioId,
        Long demandId,
        String demandName,
        Integer headcount,
        Integer weekStart,
        Integer weekEnd,
        BigDecimal hoursPerWeekPerPerson,
        String skillRequirement
) {
}
