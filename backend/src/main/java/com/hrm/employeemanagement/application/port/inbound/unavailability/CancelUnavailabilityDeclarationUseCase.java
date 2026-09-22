package com.hrm.employeemanagement.application.port.inbound.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;

public interface CancelUnavailabilityDeclarationUseCase {
    UnavailabilityDeclarationResult cancel(Long declarationId);
}
