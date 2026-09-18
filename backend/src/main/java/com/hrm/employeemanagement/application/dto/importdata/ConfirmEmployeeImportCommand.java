package com.hrm.employeemanagement.application.dto.importdata;

import java.util.Collections;
import java.util.List;

public record ConfirmEmployeeImportCommand(
        List<ImportEmployeeRowDto> rows
) {
    public ConfirmEmployeeImportCommand {
        if (rows == null) {
            rows = Collections.emptyList();
        }
    }
}
