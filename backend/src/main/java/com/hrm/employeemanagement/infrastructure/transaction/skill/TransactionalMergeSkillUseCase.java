package com.hrm.employeemanagement.infrastructure.transaction.skill;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.skill.MergeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.MergeSkillUseCase;

public class TransactionalMergeSkillUseCase implements MergeSkillUseCase {
    private final MergeSkillUseCase delegate;

    public TransactionalMergeSkillUseCase(MergeSkillUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    @Transactional // <--- Quản lý Transaction tại ranh giới Infrastructure
    public SkillResult execute(MergeSkillCommand command) {
        return delegate.execute(command);
    }
}