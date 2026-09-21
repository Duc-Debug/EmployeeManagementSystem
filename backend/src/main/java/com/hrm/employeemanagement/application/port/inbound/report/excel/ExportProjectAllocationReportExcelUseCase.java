package com.hrm.employeemanagement.application.port.inbound.report.excel;

import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelQuery;
import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelResult;

/**
 * Inbound Port cho Use Case Xuất báo cáo phân bổ dự án ra file Excel (NCL-10-CN-003).
 */
public interface ExportProjectAllocationReportExcelUseCase {

    ExportReportExcelResult export(ExportReportExcelQuery query);
}
