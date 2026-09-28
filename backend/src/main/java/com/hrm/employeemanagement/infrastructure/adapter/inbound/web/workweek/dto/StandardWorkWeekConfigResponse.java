package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StandardWorkWeekConfigResponse(
        Long id,
        String scopeType,
        Long orgUnitId,
        String scopeKey,
        String capacityUnit,
        String weekStartDay,
        BigDecimal standardHoursPerDay,
        BigDecimal standardHoursPerWeek,
        List<StandardWorkWeekDayResponse> days,
        Long createdBy,
        Long updatedBy,
        LocalDateTime updatedAt,
        Long version,
        boolean isInherited
) {}

