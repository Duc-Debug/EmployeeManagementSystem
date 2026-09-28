package com.hrm.employeemanagement.application.dto.notification.dedup;

import java.math.BigDecimal;
import java.util.List;

public record EmployeeWeeklyOverloadCandidate(
        Long employeeId,
        String employeeCode,
        String employeeName,
        int year,
        int weekNumber,
        BigDecimal allocatedHours,
        BigDecimal availableHours,
        boolean isOverloaded,
        List<Long> recipientUserIds
) {
    public EmployeeWeeklyOverloadCandidate {
        if (recipientUserIds == null) {
            recipientUserIds = List.of();
        }
    }
}
