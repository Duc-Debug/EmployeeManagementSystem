package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation.dto;

import jakarta.validation.constraints.Size;

public record CancelReservationRequest(
        @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự")
        String reason
) {}
