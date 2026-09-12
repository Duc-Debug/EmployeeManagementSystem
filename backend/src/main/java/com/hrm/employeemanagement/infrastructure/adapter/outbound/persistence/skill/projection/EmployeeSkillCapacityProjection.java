package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection;

import java.math.BigDecimal;

public interface EmployeeSkillCapacityProjection {
    Long getSkillId();
    Long getEmployeeId();
    Integer getStandardHoursPerWeek();
    Integer getProficiencyLevel();
    BigDecimal getYearsOfExperience();
}

