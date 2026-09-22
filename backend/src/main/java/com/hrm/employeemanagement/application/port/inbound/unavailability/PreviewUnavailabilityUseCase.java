package com.hrm.employeemanagement.application.port.inbound.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityPreviewResult;

import java.time.LocalDate;

public interface PreviewUnavailabilityUseCase {
    UnavailabilityPreviewResult preview(LocalDate startDate, LocalDate endDate);
}
