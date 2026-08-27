package com.hrm.employeemanagement.application.dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProjectCommand(
        String projectName,
        Long orgUnitId,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal estimatedHours,
        String description) {
}