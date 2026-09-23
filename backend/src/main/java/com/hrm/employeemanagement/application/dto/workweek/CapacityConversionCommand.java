package com.hrm.employeemanagement.application.dto.workweek;

import java.math.BigDecimal;

public record CapacityConversionCommand(
        BigDecimal value,
        String fromUnit,
        String toUnit,
        String scopeType,
        Long orgUnitId
) {}

