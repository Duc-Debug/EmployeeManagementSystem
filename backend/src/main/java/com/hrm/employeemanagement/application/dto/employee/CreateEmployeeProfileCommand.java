package com.hrm.employeemanagement.application.dto.employee;

import java.time.LocalDate;

public record CreateEmployeeProfileCommand(
    Long userId,
    Long orgUnitId,
    String employeeCode,
    String fullName,
    String professionalRole,
    LocalDate startDate,
    LocalDate contractEndDate,
    Integer standardHoursPerWeek
) {}