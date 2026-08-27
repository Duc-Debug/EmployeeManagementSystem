package com.hrm.employeemanagement.application.dto.employee;

import java.time.LocalDate;

public record UpdateEmployeeProfileCommand(
    Long employeeId,
    Long orgUnitId,
    String fullName,
    String professionalRole,
    LocalDate startDate,
    LocalDate contractEndDate,
    Integer standardHoursPerWeek,
    Long version
) {
    public UpdateEmployeeProfileCommand(Long employeeId, Long orgUnitId, String fullName,
                                        String professionalRole, LocalDate startDate,
                                        LocalDate contractEndDate, Integer standardHoursPerWeek) {
        this(employeeId, orgUnitId, fullName, professionalRole, startDate,
                contractEndDate, standardHoursPerWeek, null);
    }
}
