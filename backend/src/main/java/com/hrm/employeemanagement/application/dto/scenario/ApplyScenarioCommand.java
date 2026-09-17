package com.hrm.employeemanagement.application.dto.scenario;

import java.util.Objects;

public record ApplyScenarioCommand(
        Long scenarioId,
        Long targetProjectId,
        String note
) {
    public ApplyScenarioCommand {
        Objects.requireNonNull(scenarioId, "ID kịch bản không được để trống");
        Objects.requireNonNull(targetProjectId, "ID dự án mục tiêu không được để trống");
    }
}

