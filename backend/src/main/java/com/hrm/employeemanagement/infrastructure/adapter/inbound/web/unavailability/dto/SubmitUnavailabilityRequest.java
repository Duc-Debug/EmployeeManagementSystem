package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto;

import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SubmitUnavailabilityRequest(
        @NotNull(message = "employeeId không được để trống")
        Long employeeId,

        @NotNull(message = "startDate không được để trống")
        LocalDate startDate,

        @NotNull(message = "endDate không được để trống")
        LocalDate endDate,

        @NotNull(message = "reasonType không được để trống")
        UnavailabilityReasonType reasonType,

        String reasonDetail
) {}
