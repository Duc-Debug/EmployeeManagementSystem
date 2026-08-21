package com.hrm.employeemanagement.application.port.inbound.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.MoveOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;

public interface MoveOrgUnitUseCase {
    OrgUnitResult execute(MoveOrgUnitCommand command);
}