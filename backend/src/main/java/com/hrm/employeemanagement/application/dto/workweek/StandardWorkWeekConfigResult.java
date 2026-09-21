package com.hrm.employeemanagement.application.dto.workweek;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StandardWorkWeekConfigResult(
        Long id,
        String scopeType,
        Long orgUnitId,
        String scopeKey,
        String capacityUnit,
        String weekStartDay,
        BigDecimal standardHoursPerDay,
        BigDecimal standardHoursPerWeek,
        List<StandardWorkWeekDayDto> days,
        Long createdBy,
        Long updatedBy,
        LocalDateTime updatedAt,
        boolean isInherited
) {}

