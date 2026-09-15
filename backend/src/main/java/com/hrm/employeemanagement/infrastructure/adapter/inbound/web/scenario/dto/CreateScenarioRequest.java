package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateScenarioRequest(
        @Size(max = 50, message = "Mã kịch bản không được vượt quá 50 ký tự")
        String code,

        @NotBlank(message = "Tên kịch bản không được để trống")
        @Size(max = 255, message = "Tên kịch bản không được vượt quá 255 ký tự")
        String name,

        String description,

        Long orgUnitId,

        Integer fromYear,

        Integer fromWeek,

        Integer durationWeeks
) {
}
