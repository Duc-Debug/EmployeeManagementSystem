package com.hrm.employeemanagement.application.dto.workweek;

import java.math.BigDecimal;
import java.util.List;

public record UpdateStandardWorkWeekCommand(
        String scopeType,
        Long orgUnitId,
        String capacityUnit,
        String weekStartDay,
        BigDecimal standardHoursPerDay,
        List<StandardWorkWeekDayDto> days
) {}

