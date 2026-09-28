package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CapacityConversionRequest(
        @NotNull(message = "Giá trị cần quy đổi không được null")
        BigDecimal value,

        @NotBlank(message = "Đơn vị nguồn không được để trống")
        String fromUnit,

        @NotBlank(message = "Đơn vị đích không được để trống")
        String toUnit,

        String scopeType,
        Long orgUnitId
) {}

