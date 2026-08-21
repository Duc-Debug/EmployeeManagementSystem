package com.hrm.employeemanagement.application.port.inbound.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.CreateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;

public interface CreateOrgUnitUseCase {
    OrgUnitResult execute(CreateOrgUnitCommand command);
}
