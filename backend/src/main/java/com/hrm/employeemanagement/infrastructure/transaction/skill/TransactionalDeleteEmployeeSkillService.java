package com.hrm.employeemanagement.infrastructure.transaction.skill;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.skill.DeleteEmployeeSkillCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.DeleteEmployeeSkillUseCase;

public class TransactionalDeleteEmployeeSkillService implements DeleteEmployeeSkillUseCase {

    private final DeleteEmployeeSkillUseCase delegate;

    public TransactionalDeleteEmployeeSkillService(DeleteEmployeeSkillUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(DeleteEmployeeSkillCommand command) {
        delegate.execute(command);
    }
}
