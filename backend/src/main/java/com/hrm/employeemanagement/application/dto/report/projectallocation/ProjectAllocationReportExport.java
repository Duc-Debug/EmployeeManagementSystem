package com.hrm.employeemanagement.application.dto.report.projectallocation;

public record ProjectAllocationReportExport(
        String filename,
        byte[] content
) {
}