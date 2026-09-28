package com.hrm.employeemanagement.application.port.outbound.importdata;

import java.time.LocalDate;
import com.hrm.employeemanagement.domain.role.Role;

public interface SingleRowEmployeeImportPort {
    SingleRowImportResult importSingleRow(
            int rowNumber,
            String employeeCode,
            String fullName,
            String username,
            String encodedPassword,
            String email,
            Long resolvedOrgUnitId,
            Role role,
            Long scopeOrgUnitId,
            String professionalRole,
            int standardHours,
            LocalDate startDate,
            LocalDate contractEndDate,
            boolean isOutsourced
    );
}