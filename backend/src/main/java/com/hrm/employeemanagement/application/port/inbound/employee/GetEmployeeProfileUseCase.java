package com.hrm.employeemanagement.application.port.inbound.employee;

import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;

public interface GetEmployeeProfileUseCase {
    EmployeeProfileResult getById(Long employeeId);
    EmployeeProfileResult getByUserId(Long userId);
    PageResult<EmployeeProfileResult> getEmployees(int page, int size);
}