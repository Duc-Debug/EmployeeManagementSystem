package com.hrm.employeemanagement.domain.report.excel;

/**
 * Các loại báo cáo hỗ trợ xuất ra tệp Excel (NCL-10-CN-003).
 */
public enum ExcelReportType {
    PROJECT_ALLOCATION("Báo cáo phân bổ dự án theo tuần");

    private final String description;

    ExcelReportType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
