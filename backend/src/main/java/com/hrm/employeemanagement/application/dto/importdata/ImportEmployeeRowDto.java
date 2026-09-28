package com.hrm.employeemanagement.application.dto.importdata;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record ImportEmployeeRowDto(
        int rowNumber,
        String employeeCode,
        String fullName,
        String username,
        String email,
        String orgUnitIdentifier,
        Long resolvedOrgUnitId,
        String orgUnitName,
        String roleCode,
        String professionalRole,
        Integer standardHoursPerWeek,
        LocalDate startDate,
        LocalDate contractEndDate,
        Boolean isOutsourced,
        boolean valid,
        List<String> errors
) {
    public ImportEmployeeRowDto {
        if (errors == null) {
            errors = Collections.emptyList();
        }
    }
}
