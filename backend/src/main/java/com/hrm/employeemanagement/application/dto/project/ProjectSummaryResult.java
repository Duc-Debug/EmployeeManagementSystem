package com.hrm.employeemanagement.application.dto.project;

/**
 * DTO chứa số liệu tổng hợp trạng thái dự án (P2 Executive Dashboard KPI).
 */
public record ProjectSummaryResult(
        long totalProjects,
        long activeProjects,
        long plannedProjects,
        long closedProjects
) {}
