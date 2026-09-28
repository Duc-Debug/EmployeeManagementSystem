package com.hrm.employeemanagement.application.port.outbound.report.excel;

import com.hrm.employeemanagement.domain.report.excel.ExcelReportData;

/**
 * Outbound Port chịu trách nhiệm chuyển đổi mô hình dữ liệu báo cáo
 * thành tệp nhị phân bảng tính Excel (.xlsx).
 */
public interface GenerateExcelWorkbookPort {

    byte[] generateProjectAllocationWorkbook(ExcelReportData data);
}
