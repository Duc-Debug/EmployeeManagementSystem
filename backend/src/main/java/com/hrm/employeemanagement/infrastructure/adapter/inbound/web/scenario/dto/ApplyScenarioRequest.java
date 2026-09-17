package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto;

import jakarta.validation.constraints.NotNull;

public record ApplyScenarioRequest(
        @NotNull(message = "ID dự án mục tiêu không được để trống")
        Long targetProjectId,
        String note
) {}

