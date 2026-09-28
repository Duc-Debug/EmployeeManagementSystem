package com.hrm.employeemanagement.infrastructure.transaction.skill;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.hrm.employeemanagement.application.dto.skill.DeactivateSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.DeactivateSkillUseCase;

public class TransactionalDeactivateSkillUseCase implements DeactivateSkillUseCase {
    private final DeactivateSkillUseCase delegate;

    public TransactionalDeactivateSkillUseCase(DeactivateSkillUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    @Transactional
    public SkillResult execute(DeactivateSkillCommand command) {
        return delegate.execute(command);
    }
}
