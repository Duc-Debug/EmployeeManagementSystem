package com.hrm.employeemanagement.application.dto.reservation;

import java.math.BigDecimal;

public record CreateReservationCommand(
        Long projectId,
        Long employeeId,
        int year,
        int weekNumber,
        BigDecimal reservedHours,
        String note
) {}
