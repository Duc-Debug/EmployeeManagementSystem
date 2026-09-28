package com.hrm.employeemanagement.application.dto.scenario;

public record CreateScenarioCommand(
        String code,
        String name,
        String description,
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks
) {
}
