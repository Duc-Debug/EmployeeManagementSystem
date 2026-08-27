package com.hrm.employeemanagement.application.port.inbound.employee;

import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;

public interface GetEmployeeProfileUseCase {
    EmployeeProfileResult getById(Long employeeId);
    EmployeeProfileResult getByUserId(Long userId);
}