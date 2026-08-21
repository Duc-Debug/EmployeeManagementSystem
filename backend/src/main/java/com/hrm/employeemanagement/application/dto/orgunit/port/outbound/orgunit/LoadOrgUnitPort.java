package com.hrm.employeemanagement.application.dto.orgunit.port.outbound.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import java.util.List;
import java.util.Optional;

public interface LoadOrgUnitPort {
    Optional<OrgUnit> findById(OrgUnitId id);

    Optional<OrgUnit> findByUnitCode(String unitCode);

    boolean existsByUnitCode(String unitCode);

    List<OrgUnit> findAllActive();

    List<OrgUnit> findSubTree(String treePath);
}
