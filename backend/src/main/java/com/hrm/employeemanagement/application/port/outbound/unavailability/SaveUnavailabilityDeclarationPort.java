package com.hrm.employeemanagement.application.port.outbound.unavailability;

import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;

public interface SaveUnavailabilityDeclarationPort {
    UnavailabilityDeclaration save(UnavailabilityDeclaration declaration);
}
