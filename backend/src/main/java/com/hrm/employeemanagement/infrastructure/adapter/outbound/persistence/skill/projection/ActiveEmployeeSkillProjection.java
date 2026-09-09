package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ActiveEmployeeSkillProjection {
    Long getEmployeeId();
    Long getUserId();
    String getEmployeeCode();
    String getFullName();
    Long getOrgUnitId();
    String getProfessionalRole();
    Integer getStandardHoursPerWeek();
    LocalDate getContractEndDate();
    Long getSkillId();
    String getSkillName();
    Integer getProficiencyLevel();
    BigDecimal getYearsOfExperience();
}
