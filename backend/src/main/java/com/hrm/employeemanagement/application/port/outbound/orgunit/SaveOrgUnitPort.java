package com.hrm.employeemanagement.application.port.outbound.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnit;

public interface SaveOrgUnitPort {
    OrgUnit save(OrgUnit orgUnit);

    int updateSubTreePaths(String oldPrefix, String newPrefix, int levelDelta);

    int deactivateSubTree(String treePath);
}