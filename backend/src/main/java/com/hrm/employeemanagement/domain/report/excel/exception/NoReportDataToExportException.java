package com.hrm.employeemanagement.domain.report.excel.exception;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi không có số liệu để xuất báo cáo trong kỳ (NCL-10-CN-003-TC-02).
 */
public class NoReportDataToExportException extends DomainException {

    public NoReportDataToExportException(String message) {
        super(message);
    }
}
