package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;

public record AddScenarioDemandCommand(
        Long scenarioId,
        String demandName,
        Integer headcount,
        Integer startYear,
        Integer startWeek,
        Integer endYear,
        Integer endWeek,
        BigDecimal hoursPerWeekPerPerson,
        String skillRequirement
) {
}
