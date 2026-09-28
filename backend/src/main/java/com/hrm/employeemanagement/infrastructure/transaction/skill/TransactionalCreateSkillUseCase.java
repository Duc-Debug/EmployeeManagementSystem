package com.hrm.employeemanagement.infrastructure.transaction.skill;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.hrm.employeemanagement.application.dto.skill.CreateSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.CreateSkillUseCase;

public class TransactionalCreateSkillUseCase implements CreateSkillUseCase {
    private final CreateSkillUseCase delegate;

    public TransactionalCreateSkillUseCase(CreateSkillUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    @Transactional
    public SkillResult execute(CreateSkillCommand command) {
        return delegate.execute(command);
    }
}
