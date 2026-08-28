package com.hrm.employeemanagement.infrastructure.transaction.skill;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.skill.DeclareEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.DeclareEmployeeSkillUseCase;

public class TransactionalDeclareEmployeeSkillService implements DeclareEmployeeSkillUseCase {

    private final DeclareEmployeeSkillUseCase delegate;

    public TransactionalDeclareEmployeeSkillService(DeclareEmployeeSkillUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public EmployeeSkillResult execute(DeclareEmployeeSkillCommand command) {
        return delegate.execute(command);
    }
}
