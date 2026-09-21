package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto;

import java.math.BigDecimal;

public record CapacityConversionResponse(
        BigDecimal originalValue,
        String fromUnit,
        BigDecimal convertedValue,
        String toUnit,
        String formulaDescription
) {}

