package com.hrm.employeemanagement.application.dto.workweek;

import java.math.BigDecimal;

public record CapacityConversionResult(
        BigDecimal originalValue,
        String fromUnit,
        BigDecimal convertedValue,
        String toUnit,
        String formulaDescription
) {}

