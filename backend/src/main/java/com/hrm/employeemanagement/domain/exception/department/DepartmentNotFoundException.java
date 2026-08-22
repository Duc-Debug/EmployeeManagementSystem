package com.hrm.employeemanagement.domain.exception.department;

public class DepartmentNotFoundException extends RuntimeException {

    public DepartmentNotFoundException(Long departmentId) {
        super("Không tìm thấy phòng ban với ID: " + departmentId);
    }
}