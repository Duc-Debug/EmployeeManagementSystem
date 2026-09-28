package com.hrm.employeemanagement.application.port.inbound.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.ActivateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;

public interface ActivateOrgUnitUseCase {
    OrgUnitResult execute(ActivateOrgUnitCommand command);
}
