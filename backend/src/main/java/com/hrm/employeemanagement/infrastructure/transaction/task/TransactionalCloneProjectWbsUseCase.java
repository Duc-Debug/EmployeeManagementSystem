package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsCommand;
import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsResult;
import com.hrm.employeemanagement.application.port.inbound.task.CloneProjectWbsUseCase;

public class TransactionalCloneProjectWbsUseCase implements CloneProjectWbsUseCase {

    private final CloneProjectWbsUseCase delegate;

    public TransactionalCloneProjectWbsUseCase(CloneProjectWbsUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate CloneProjectWbsUseCase must not be null");
    }

    @Override
    @Transactional
    public CloneProjectWbsResult cloneWbs(CloneProjectWbsCommand command) {
        return delegate.cloneWbs(command);
    }
}
