package com.hrm.employeemanagement.application.dto.orgunit.port.inbound.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.DeactivateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;

public interface DeactivateOrgUnitUseCase {
    OrgUnitResult execute(DeactivateOrgUnitCommand command);
}