package com.hrm.employeemanagement.infrastructure.transaction.skill;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateSkillCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.UpdateSkillUseCase;

public class TransactionalUpdateSkillUseCase implements UpdateSkillUseCase {
    private final UpdateSkillUseCase delegate;

    public TransactionalUpdateSkillUseCase(UpdateSkillUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    @Transactional
    public SkillResult execute(UpdateSkillCommand command) {
        return delegate.execute(command);
    }
}
