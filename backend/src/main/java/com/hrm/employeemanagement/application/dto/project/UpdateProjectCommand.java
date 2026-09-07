package com.hrm.employeemanagement.application.dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProjectCommand(
        Long projectId,
        String projectName,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal estimatedHours,
        String description) {
}