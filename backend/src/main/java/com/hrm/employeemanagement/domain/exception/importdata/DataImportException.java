package com.hrm.employeemanagement.domain.exception.importdata;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Bắt lỗi gặp khi import dữ liệu nhân viên vào
 */
public class DataImportException extends DomainException {
    public DataImportException(String message) {
        super(message);
    }

    public DataImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
