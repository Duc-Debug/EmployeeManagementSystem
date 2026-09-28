package com.hrm.employeemanagement.application.port.inbound.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.ApproveUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;

public interface ApproveUnavailabilityDeclarationUseCase {
    UnavailabilityDeclarationResult approve(ApproveUnavailabilityCommand command);
}
