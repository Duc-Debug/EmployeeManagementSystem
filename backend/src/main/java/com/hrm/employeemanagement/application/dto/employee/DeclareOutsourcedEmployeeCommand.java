package com.hrm.employeemanagement.application.dto.employee;

import java.time.LocalDate;
import java.util.List;

public record DeclareOutsourcedEmployeeCommand(
    Long orgUnitId,
    String employeeCode,
    String fullName,
    String providerName,
    String professionalRole,
    LocalDate startDate,
    LocalDate contractEndDate,
    Integer standardHoursPerWeek,
    List<Long> skillIds
) {}
