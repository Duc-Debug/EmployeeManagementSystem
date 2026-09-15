package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScenarioDemandResult(
        Long id,
        Long scenarioId,
        String demandName,
        Integer headcount,
        Integer weekStart,
        Integer weekEnd,
        BigDecimal hoursPerWeekPerPerson,
        BigDecimal totalHoursPerWeek,
        String skillRequirement,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
