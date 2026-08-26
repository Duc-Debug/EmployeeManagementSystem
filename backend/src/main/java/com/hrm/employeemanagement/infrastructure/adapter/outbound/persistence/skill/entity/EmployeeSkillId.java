package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity;

import java.io.Serializable;
import java.util.Objects;

public class EmployeeSkillId implements Serializable {
    private Long employeeId;
    private Long skillId;

    public EmployeeSkillId() {
    }

    public EmployeeSkillId(Long employeeId, Long skillId) {
        this.employeeId = employeeId;
        this.skillId = skillId;
    }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeeSkillId that = (EmployeeSkillId) o;
        return Objects.equals(employeeId, that.employeeId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, skillId);
    }
}
