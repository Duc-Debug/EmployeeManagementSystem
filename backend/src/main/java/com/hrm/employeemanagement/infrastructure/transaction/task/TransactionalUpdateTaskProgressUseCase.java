package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.TaskProgressResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskProgressCommand;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskProgressUseCase;

public class TransactionalUpdateTaskProgressUseCase implements UpdateTaskProgressUseCase {

    private final UpdateTaskProgressUseCase delegate;

    public TransactionalUpdateTaskProgressUseCase(UpdateTaskProgressUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate UpdateTaskProgressUseCase must not be null");
    }

    @Override
    @Transactional
    public TaskProgressResult updateProgress(UpdateTaskProgressCommand command) {
        return delegate.updateProgress(command);
    }
}
