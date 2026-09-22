package com.hrm.employeemanagement.application.port.inbound.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.RejectUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;

public interface RejectUnavailabilityDeclarationUseCase {
    UnavailabilityDeclarationResult reject(RejectUnavailabilityCommand command);
}
