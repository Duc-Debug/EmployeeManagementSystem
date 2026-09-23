package com.hrm.employeemanagement.infrastructure.transaction.employee;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.employee.DeclareOutsourcedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.employee.OutsourcedEmployeeResult;
import com.hrm.employeemanagement.application.port.inbound.employee.DeclareOutsourcedEmployeeUseCase;

public class TransactionalDeclareOutsourcedEmployeeServiceDecorator implements DeclareOutsourcedEmployeeUseCase {

    private final DeclareOutsourcedEmployeeUseCase delegate;

    public TransactionalDeclareOutsourcedEmployeeServiceDecorator(DeclareOutsourcedEmployeeUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public OutsourcedEmployeeResult execute(DeclareOutsourcedEmployeeCommand command) {
        return delegate.execute(command);
    }
}
