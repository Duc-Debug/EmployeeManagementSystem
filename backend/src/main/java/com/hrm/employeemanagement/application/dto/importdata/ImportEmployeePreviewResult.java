package com.hrm.employeemanagement.application.dto.importdata;

import java.util.List;

public record ImportEmployeePreviewResult(
        int totalRows,
        int validRows,
        int invalidRows,
        List<ImportEmployeeRowDto> rows,
        boolean canProceed,
        String message
) {}
