package com.hrm.employeemanagement.domain.report;

import java.util.List;

/**
 * A weekly capacity is an employee property, not a skill property.  The current
 * employee-skill declaration model has no primary/reporting-skill designation,
 * therefore only an employee with exactly one approved active skill can be
 * attributed to a skill in this report.  Multi-skill capacity is returned as
 * unattributed instead of being double counted or assigned by a heuristic.
 */
public final class EmployeeCapacityAttributionPolicy {

    private EmployeeCapacityAttributionPolicy() {
    }

    public static Long attributedSkillId(List<Long> approvedSkillIds) {
        if (approvedSkillIds == null || approvedSkillIds.size() != 1) {
            return null;
        }
        return approvedSkillIds.get(0);
    }
}
