package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection;

public interface EmployeeSkillCapacityProjection {
    Long getSkillId();
    Long getEmployeeId();
    Integer getStandardHoursPerWeek();
}
