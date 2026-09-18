package com.hrm.employeemanagement.application.dto.scenario;

import java.util.Objects;

public record ApplyScenarioCommand(
        Long scenarioId,
        Long targetProjectId,
        String note,
        Boolean allowPartialFulfillment
) {
    public ApplyScenarioCommand(Long scenarioId, Long targetProjectId, String note) {
        this(scenarioId, targetProjectId, note, false);
    }

    public ApplyScenarioCommand {
        Objects.requireNonNull(scenarioId, "ID kịch bản không được để trống");
        Objects.requireNonNull(targetProjectId, "ID dự án mục tiêu không được để trống");
        allowPartialFulfillment = allowPartialFulfillment != null ? allowPartialFulfillment : false;
    }

    public boolean isAllowPartialFulfillment() {
        return Boolean.TRUE.equals(allowPartialFulfillment);
    }
}
