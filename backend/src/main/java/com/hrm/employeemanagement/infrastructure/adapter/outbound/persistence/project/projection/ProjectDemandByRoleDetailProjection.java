package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.projection;

import java.math.BigDecimal;

public interface ProjectDemandByRoleDetailProjection {
    Long getRoleId();
    BigDecimal getRequiredHours();
    String getProjectCode();
    String getProjectName();
}
