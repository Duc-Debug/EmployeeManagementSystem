package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.math.BigDecimal;
import java.util.Objects;

public record RoleShortfallDemand(
        Long roleId,
        String roleCode,
        String roleName,
        BigDecimal shortfallHours
) {
    public RoleShortfallDemand {
        Objects.requireNonNull(roleId, "roleId must not be null");
        shortfallHours = shortfallHours != null ? shortfallHours : BigDecimal.ZERO;
    }
}
