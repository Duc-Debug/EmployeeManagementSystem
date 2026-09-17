package com.hrm.employeemanagement.application.dto.scenario;

import java.time.LocalDateTime;

public record ScenarioResult(
        Long id,
        String code,
        String name,
        String description,
        Long orgUnitId,
        String orgUnitName,
        String status,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks,
        LocalDateTime baseSnapshotAt,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int demandsCount,
        int snapshotEmployeesCount,
        Long targetProjectId,
        LocalDateTime appliedAt,
        Long appliedBy
) {
    public ScenarioResult(
            Long id,
            String code,
            String name,
            String description,
            Long orgUnitId,
            String orgUnitName,
            String status,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            LocalDateTime baseSnapshotAt,
            Long createdBy,
            String createdByName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int demandsCount,
            int snapshotEmployeesCount
    ) {
        this(id, code, name, description, orgUnitId, orgUnitName, status, fromYear, fromWeek, durationWeeks,
                baseSnapshotAt, createdBy, createdByName, createdAt, updatedAt, demandsCount, snapshotEmployeesCount,
                null, null, null);
    }
}
