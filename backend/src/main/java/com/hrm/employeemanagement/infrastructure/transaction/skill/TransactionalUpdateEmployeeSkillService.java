package com.hrm.employeemanagement.infrastructure.transaction.skill;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateEmployeeSkillCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.UpdateEmployeeSkillUseCase;

public class TransactionalUpdateEmployeeSkillService implements UpdateEmployeeSkillUseCase {

    private final UpdateEmployeeSkillUseCase delegate;

    public TransactionalUpdateEmployeeSkillService(UpdateEmployeeSkillUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public EmployeeSkillResult execute(UpdateEmployeeSkillCommand command) {
        return delegate.execute(command);
    }
}
