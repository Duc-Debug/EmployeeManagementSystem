package com.hrm.employeemanagement.application.dto.availability;

public record CalculateWeeklyCapacityQuery(
        Long employeeId,
        Integer year,
        Integer weekNumber
) {}
