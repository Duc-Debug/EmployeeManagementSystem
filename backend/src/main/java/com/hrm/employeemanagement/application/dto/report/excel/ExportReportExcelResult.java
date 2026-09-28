package com.hrm.employeemanagement.application.dto.report.excel;

import java.util.Objects;

/**
 * Result DTO chứa tệp nhị phân Excel (.xlsx) sau khi xuất thành công.
 */
public record ExportReportExcelResult(
        String filename,
        byte[] content,
        String contentType
) {
    public ExportReportExcelResult {
        Objects.requireNonNull(filename, "filename không được null");
        Objects.requireNonNull(content, "content không được null");
        contentType = contentType != null ? contentType : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}
