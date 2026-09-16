package com.hrm.employeemanagement.application.dto.scenario;

import java.time.LocalDateTime;

public record ScenarioShareResult(
        Long id,
        Long scenarioId,
        Long sharedWithUserId,
        String sharedWithUsername,
        String sharedWithFullName,
        String sharedWithRoleCode,
        String sharedWithRoleName,
        Long sharedByUserId,
        String accessLevel,
        LocalDateTime createdAt,
        LocalDateTime revokedAt,
        boolean active
) {
}
