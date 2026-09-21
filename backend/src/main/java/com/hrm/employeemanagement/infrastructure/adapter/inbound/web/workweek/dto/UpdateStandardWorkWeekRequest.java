package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record UpdateStandardWorkWeekRequest(
        String scopeType,
        Long orgUnitId,
        String capacityUnit,
        String weekStartDay,
        BigDecimal standardHoursPerDay,

        @NotNull(message = "Danh sách ngày làm việc không được null")
        @NotEmpty(message = "Danh sách ngày làm việc không được rỗng")
        @Valid
        List<StandardWorkWeekDayRequest> days,

        Long version
) {}

