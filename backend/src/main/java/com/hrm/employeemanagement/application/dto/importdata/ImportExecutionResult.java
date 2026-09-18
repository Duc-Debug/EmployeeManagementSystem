package com.hrm.employeemanagement.application.dto.importdata;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record ImportExecutionResult(
        int importedCount,
        int skippedCount,
        List<String> errors,
        String message,
        LocalDateTime executedAt
) {
    public ImportExecutionResult {
        if (errors == null) {
            errors = Collections.emptyList();
        }
    }
}
