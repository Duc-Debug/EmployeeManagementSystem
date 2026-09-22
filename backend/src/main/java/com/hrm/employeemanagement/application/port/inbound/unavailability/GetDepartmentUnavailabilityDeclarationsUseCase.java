package com.hrm.employeemanagement.application.port.inbound.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;

import java.util.List;

public interface GetDepartmentUnavailabilityDeclarationsUseCase {
    List<UnavailabilityDeclarationResult> getPendingDeclarations(Long orgUnitId);
}
