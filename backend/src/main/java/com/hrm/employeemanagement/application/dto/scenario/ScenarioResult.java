package com.hrm.employeemanagement.application.dto.scenario;

import java.time.LocalDateTime;

public record ScenarioResult(
        Long id,
        String code,
        String name,
        String description,
        String note,
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
        String viewMode
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
        this(id, code, name, description, null, orgUnitId, orgUnitName, status, fromYear, fromWeek, durationWeeks, baseSnapshotAt, createdBy, createdByName, createdAt, updatedAt, demandsCount, snapshotEmployeesCount, "EDIT");
    }

    public ScenarioResult withViewMode(String newViewMode) {
        return new ScenarioResult(
                id, code, name, description, note, orgUnitId, orgUnitName, status,
                fromYear, fromWeek, durationWeeks, baseSnapshotAt, createdBy, createdByName,
                createdAt, updatedAt, demandsCount, snapshotEmployeesCount, newViewMode
        );
    }
}
