package com.hrm.employeemanagement.application.dto.unavailability;

import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;

import java.time.LocalDate;

public record SubmitUnavailabilityCommand(
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        UnavailabilityReasonType reasonType,
        String reasonDetail
) {}
