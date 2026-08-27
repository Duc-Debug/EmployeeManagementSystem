package com.hrm.employeemanagement.infrastructure.transaction.skill;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.hrm.employeemanagement.application.dto.skill.CreateSkillGroupCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;
import com.hrm.employeemanagement.application.port.inbound.skill.CreateSkillGroupUseCase;

public class TransactionalCreateSkillGroupUseCase implements CreateSkillGroupUseCase {
    private final CreateSkillGroupUseCase delegate;

    public TransactionalCreateSkillGroupUseCase(CreateSkillGroupUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    @Transactional
    public SkillGroupResult execute(CreateSkillGroupCommand command) {
        return delegate.execute(command);
    }
}
