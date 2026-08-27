package com.hrm.employeemanagement.infrastructure.transaction.employee;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.employee.CreateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;
import com.hrm.employeemanagement.application.dto.employee.UpdateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.port.inbound.employee.CreateEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.GetEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.UpdateEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.service.employee.EmployeeProfileService;

public class TransactionalEmployeeProfileServiceDecorator implements CreateEmployeeProfileUseCase,
        UpdateEmployeeProfileUseCase, GetEmployeeProfileUseCase {

    private final EmployeeProfileService delegate;

    public TransactionalEmployeeProfileServiceDecorator(EmployeeProfileService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public EmployeeProfileResult execute(CreateEmployeeProfileCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public EmployeeProfileResult execute(UpdateEmployeeProfileCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileResult getById(Long employeeId) {
        return delegate.getById(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileResult getByUserId(Long userId) {
        return delegate.getByUserId(userId);
    }
}
