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
        List<String> managedProjectNames
) {
}
