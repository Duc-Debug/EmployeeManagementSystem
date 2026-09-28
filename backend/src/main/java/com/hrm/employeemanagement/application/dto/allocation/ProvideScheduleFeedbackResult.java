package com.hrm.employeemanagement.application.dto.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProvideScheduleFeedbackResult(
        int httpStatusCode,
        LocalDate weekStartDate,
        LocalDateTime feedbackAt,
        String feedbackNote,
        String confirmationStatus,
        String message
) {}
