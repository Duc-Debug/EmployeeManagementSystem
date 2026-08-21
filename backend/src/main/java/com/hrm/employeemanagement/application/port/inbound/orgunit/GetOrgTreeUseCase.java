package com.hrm.employeemanagement.application.port.inbound.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitNodeResult;
import java.util.List;
public interface GetOrgTreeUseCase {
    List<OrgUnitNodeResult> execute();
}