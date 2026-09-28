package com.hrm.employeemanagement.application.dto.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConfirmScheduleViewedResult(
        int httpStatusCode,
        LocalDate weekStartDate,
        LocalDateTime confirmedAt,
        String confirmationStatus,
        boolean alreadyConfirmed,
        Boolean previousConfirmationWasStale
) {}