package com.hrm.employeemanagement.application.dto.report.billablerate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BillableRateResult(
        Long orgUnitId,
        String orgUnitName,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        LocalDate startDate,
        LocalDate endDate,
        List<DepartmentBillableRateSummary> departmentBreakdown,
        List<BillableRateItem> employeeBreakdown,
        BillableRateSummary summary,
        boolean hasData,
        String message,
        LocalDateTime generatedAt
) {
}
