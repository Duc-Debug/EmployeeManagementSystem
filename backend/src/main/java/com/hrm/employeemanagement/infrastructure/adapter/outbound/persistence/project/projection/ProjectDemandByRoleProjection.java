package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.projection;

import java.math.BigDecimal;

public interface ProjectDemandByRoleProjection {
    Long getRoleId();
    BigDecimal getRequiredHours();
}
