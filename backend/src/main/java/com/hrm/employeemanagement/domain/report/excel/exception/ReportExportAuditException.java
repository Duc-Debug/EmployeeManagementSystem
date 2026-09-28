package com.hrm.employeemanagement.domain.report.excel.exception;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi vi phạm quy tắc QTN-02:
 * Không cho hoàn tất thao tác xuất dữ liệu nếu không thể ghi nhật ký kiểm toán.
 */
public class ReportExportAuditException extends DomainException {

    public ReportExportAuditException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportExportAuditException(String message) {
        super(message);
    }
}
