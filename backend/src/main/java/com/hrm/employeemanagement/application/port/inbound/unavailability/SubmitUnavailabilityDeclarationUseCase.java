package com.hrm.employeemanagement.application.port.inbound.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.SubmitUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;

public interface SubmitUnavailabilityDeclarationUseCase {
    UnavailabilityDeclarationResult submit(SubmitUnavailabilityCommand command);
}
