package com.hrm.employeemanagement.domain.exception.employee;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateEmployeeCodeException extends DomainException {
    public DuplicateEmployeeCodeException(String employeeCode) {
        super("Mã nhân viên '" + employeeCode + "' đã tồn tại trong hệ thống");
    }
}