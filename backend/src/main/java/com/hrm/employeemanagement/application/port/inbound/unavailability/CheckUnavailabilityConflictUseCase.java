package com.hrm.employeemanagement.application.port.inbound.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult;

public interface CheckUnavailabilityConflictUseCase {
    UnavailabilityConflictCheckResult checkConflict(Long declarationId);
}
