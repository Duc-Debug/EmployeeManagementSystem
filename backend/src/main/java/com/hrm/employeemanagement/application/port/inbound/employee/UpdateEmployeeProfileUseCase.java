package com.hrm.employeemanagement.application.port.inbound.employee;

import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;
import com.hrm.employeemanagement.application.dto.employee.UpdateEmployeeProfileCommand;

public interface UpdateEmployeeProfileUseCase {
    EmployeeProfileResult execute(UpdateEmployeeProfileCommand command);
}