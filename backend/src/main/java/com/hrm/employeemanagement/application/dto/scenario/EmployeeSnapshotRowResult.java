package com.hrm.employeemanagement.application.dto.scenario;

import java.util.List;

public record EmployeeSnapshotRowResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        String professionalRole,
        List<EmployeeSnapshotCellResult> cells
) {
}
