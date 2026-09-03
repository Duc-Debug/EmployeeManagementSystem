package com.hrm.employeemanagement.infrastructure.transaction.skill;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateSkillGroupCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.UpdateSkillGroupUseCase;

public class TransactionalUpdateSkillGroupUseCase implements UpdateSkillGroupUseCase {
    private final UpdateSkillGroupUseCase delegate;

    public TransactionalUpdateSkillGroupUseCase(UpdateSkillGroupUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    @Transactional
    public SkillGroupResult execute(UpdateSkillGroupCommand command) {
        return delegate.execute(command);
    }
}
