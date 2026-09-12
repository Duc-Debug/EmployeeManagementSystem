package com.hrm.employeemanagement.application.dto.reservation;

import com.hrm.employeemanagement.domain.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResourceReservationResult(
        Long id,
        Long projectId,
        String projectCode,
        String projectName,
        Long employeeId,
        String employeeCode,
        String employeeFullName,
        int year,
        int weekNumber,
        BigDecimal reservedHours,
        ReservationStatus status,
        Long convertedAllocationId,
        String cancelledReason,
        String note,
        Long createdBy,
        LocalDateTime createdAt
) {}
