package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompareScenariosRequest(
        @NotNull(message = "Danh sách mã kịch bản không được để trống")
        @NotEmpty(message = "Cần ít nhất hai kịch bản để so sánh")
        @Size(min = 2, message = "Cần ít nhất hai kịch bản để so sánh")
        List<Long> scenarioIds
) {
}
