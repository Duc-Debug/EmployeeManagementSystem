package com.hrm.employeemanagement.application.dto.report.billablerate;

public record BillableRateExport(
        String filename,
        byte[] content
) {
}
