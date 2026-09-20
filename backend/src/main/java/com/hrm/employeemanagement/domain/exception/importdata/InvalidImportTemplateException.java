package com.hrm.employeemanagement.domain.exception.importdata;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Bắt lỗi khi trường dữ liệu import vào không chính xác
 */
public class InvalidImportTemplateException extends DomainException {
    public InvalidImportTemplateException(String message) {
        super(message);
    }
}
