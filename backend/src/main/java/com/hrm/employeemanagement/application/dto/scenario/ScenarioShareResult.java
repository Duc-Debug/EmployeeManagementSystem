package com.hrm.employeemanagement.application.dto.scenario;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ScenarioShareResult(
        Long id,
        Long scenarioId,
        Long sharedWithUserId,
        String sharedWithUsername,
        String sharedWithFullName,
        String sharedWithRoleCode,
        String sharedWithRoleName,
        Long sharedByUserId,
        String sharedByName,
        String accessLevel,
        LocalDateTime createdAt,
        LocalDateTime revokedAt,
        boolean active
) {
    public ScenarioShareResult(
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
        this(id, scenarioId, sharedWithUserId, sharedWithUsername, sharedWithFullName,
                sharedWithRoleCode, sharedWithRoleName, sharedByUserId, null, accessLevel,
                createdAt, revokedAt, active);
    }

    @JsonProperty("userId")
    public Long userId() {
        return sharedWithUserId;
    }

    @JsonProperty("username")
    public String username() {
        return sharedWithUsername;
    }

    @JsonProperty("fullName")
    public String fullName() {
        return sharedWithFullName;
    }

    @JsonProperty("roleCode")
    public String roleCode() {
        return sharedWithRoleCode;
    }

    @JsonProperty("roleName")
    public String roleName() {
        return sharedWithRoleName;
    }

    @JsonProperty("permission")
    public String permission() {
        return accessLevel;
    }

    @JsonProperty("sharedBy")
    public Long sharedBy() {
        return sharedByUserId;
    }

    @JsonProperty("sharedAt")
    public LocalDateTime sharedAt() {
        return createdAt;
    }

    @JsonProperty("isActive")
    public boolean isActive() {
        return active;
    }
}
