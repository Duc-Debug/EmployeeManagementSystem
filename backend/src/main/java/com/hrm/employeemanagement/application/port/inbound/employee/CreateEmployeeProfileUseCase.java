package com.hrm.employeemanagement.application.port.inbound.employee;

import com.hrm.employeemanagement.application.dto.employee.CreateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;

public interface CreateEmployeeProfileUseCase {
    EmployeeProfileResult execute(CreateEmployeeProfileCommand command);
}