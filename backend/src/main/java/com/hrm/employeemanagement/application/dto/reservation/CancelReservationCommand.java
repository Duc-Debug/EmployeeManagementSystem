package com.hrm.employeemanagement.application.dto.reservation;

public record CancelReservationCommand(
        Long reservationId,
        String reason
) {}
