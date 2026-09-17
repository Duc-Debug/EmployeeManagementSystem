package com.hrm.employeemanagement.application.dto.scenario;

import java.util.List;

public record ShareCandidateResult(
        Long userId,
        String username,
        String fullName,
        String employeeCode,
        String email,
        String roleCode,
        String roleName,
        Long orgUnitId,
        String orgUnitName,
        List<Long> managedProjectIds,
        List<String> managedProjectNames
) {
    public ShareCandidateResult(
            Long userId,
            String username,
            String fullName,
            String employeeCode,
            String email,
            String roleCode,
            String roleName,
            Long orgUnitId,
            String orgUnitName,
            List<String> managedProjectNames
    ) {
        this(userId, username, fullName, employeeCode, email, roleCode, roleName, orgUnitId, orgUnitName, List.of(), managedProjectNames);
    }
}
