package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PendingEmployeeSkillProjection {
    Long getId();
    Long getEmployeeId();
    String getEmployeeCode();
    String getEmployeeName();
    Long getOrgUnitId();
    String getOrgUnitName();
    Long getSkillId();
    String getSkillCode();
    String getSkillName();
    String getSkillCategory();
    Integer getProficiencyLevel();
    BigDecimal getYearsOfExperience();
    String getStatus();
    LocalDateTime getCreatedAt();
}

