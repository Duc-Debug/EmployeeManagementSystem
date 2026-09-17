package com.hrm.employeemanagement.application.dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hrm.employeemanagement.domain.project.ProjectStatus;

public record CreateProjectCommand(
        String projectName,
        Long orgUnitId,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal estimatedHours,
        String description,
        ProjectStatus status) {

    public CreateProjectCommand(
            String projectName,
            Long orgUnitId,
            Long managerId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal estimatedHours,
            String description) {
        this(projectName, orgUnitId, managerId, startDate, endDate, estimatedHours, description, ProjectStatus.ACTIVE);
    }
}