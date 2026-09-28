package com.hrm.employeemanagement.application.port.inbound.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.dto.orgunit.UpdateOrgUnitCommand;

public interface UpdateOrgUnitUseCase {
    OrgUnitResult execute(UpdateOrgUnitCommand command);
}