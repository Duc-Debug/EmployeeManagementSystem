package com.hrm.employeemanagement.infrastructure.transaction.skill;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.skill.ApproveEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.ApproveEmployeeSkillUseCase;

public class TransactionalApproveEmployeeSkillService implements ApproveEmployeeSkillUseCase {

    private final ApproveEmployeeSkillUseCase delegate;

    public TransactionalApproveEmployeeSkillService(ApproveEmployeeSkillUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public EmployeeSkillResult execute(ApproveEmployeeSkillCommand command) {
        return delegate.execute(command);
    }
    @Override
    @Transactional
    public EmployeeSkillResult reject(com.hrm.employeemanagement.application.dto.skill.RejectEmployeeSkillCommand command) {
        return delegate.reject(command);
    }
}
