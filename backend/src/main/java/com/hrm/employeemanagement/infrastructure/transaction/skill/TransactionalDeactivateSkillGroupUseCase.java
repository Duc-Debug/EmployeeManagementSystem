package com.hrm.employeemanagement.infrastructure.transaction.skill;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.hrm.employeemanagement.application.dto.skill.DeactivateSkillGroupCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;
import com.hrm.employeemanagement.application.port.inbound.skill.DeactivateSkillGroupUseCase;

public class TransactionalDeactivateSkillGroupUseCase implements DeactivateSkillGroupUseCase {
    private final DeactivateSkillGroupUseCase delegate;

    public TransactionalDeactivateSkillGroupUseCase(DeactivateSkillGroupUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    @Transactional
    public SkillGroupResult execute(DeactivateSkillGroupCommand command) {
        return delegate.execute(command);
    }
}
