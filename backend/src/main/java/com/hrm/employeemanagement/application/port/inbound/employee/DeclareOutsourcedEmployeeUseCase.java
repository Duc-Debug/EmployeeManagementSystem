package com.hrm.employeemanagement.application.port.inbound.employee;

import com.hrm.employeemanagement.application.dto.employee.DeclareOutsourcedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.employee.OutsourcedEmployeeResult;

public interface DeclareOutsourcedEmployeeUseCase {
    OutsourcedEmployeeResult execute(DeclareOutsourcedEmployeeCommand command);
}
