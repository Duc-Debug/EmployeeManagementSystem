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
        Long targetProjectId,
        LocalDateTime appliedAt,
        Long appliedBy,
        String viewMode
) {
    /**
     * Constructor tương thích ngược (17 tham số cơ bản)
     */
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
        this(id, code, name, description, null, orgUnitId, orgUnitName, status, fromYear, fromWeek, durationWeeks,
                baseSnapshotAt, createdBy, createdByName, createdAt, updatedAt, demandsCount, snapshotEmployeesCount,
                null, null, null, "EDIT");
    }

    /**
     * Constructor tương thích cho các lệnh gọi từ tính năng Áp dụng kịch bản (20 tham số)
     */
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
            int snapshotEmployeesCount,
            Long targetProjectId,
            LocalDateTime appliedAt,
            Long appliedBy
    ) {
        this(id, code, name, description, null, orgUnitId, orgUnitName, status, fromYear, fromWeek, durationWeeks,
                baseSnapshotAt, createdBy, createdByName, createdAt, updatedAt, demandsCount, snapshotEmployeesCount,
                targetProjectId, appliedAt, appliedBy, "EDIT");
    }

    public ScenarioResult withViewMode(String newViewMode) {
        return new ScenarioResult(
                id, code, name, description, note, orgUnitId, orgUnitName, status,
                fromYear, fromWeek, durationWeeks, baseSnapshotAt, createdBy, createdByName,
                createdAt, updatedAt, demandsCount, snapshotEmployeesCount,
                targetProjectId, appliedAt, appliedBy, newViewMode
        );
    }
}