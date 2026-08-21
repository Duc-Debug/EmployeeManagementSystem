package com.hrm.employeemanagement.application.dto.orgunit.port.outbound.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnit;

public interface SaveOrgUnitPort {
    OrgUnit save(OrgUnit orgUnit);
}