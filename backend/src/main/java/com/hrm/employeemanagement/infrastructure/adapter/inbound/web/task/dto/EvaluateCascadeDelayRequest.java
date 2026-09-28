package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record EvaluateCascadeDelayRequest(
        @NotNull(message = "Ngày kết thúc thực tế mới không được để trống")
        LocalDate newActualEndDate
) {
}
