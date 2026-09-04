package com.hrm.employeemanagement.application.dto.availability;

public record DeclareWeeklyAvailabilityCommand(
        Long employeeId,
        Integer year,
        Integer weekNumber,
        Integer standardHours
) {}
