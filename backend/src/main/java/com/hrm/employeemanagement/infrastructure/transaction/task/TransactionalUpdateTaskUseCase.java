package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskCommand;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskUseCase;

public class TransactionalUpdateTaskUseCase implements UpdateTaskUseCase {

    private final UpdateTaskUseCase delegate;

    public TransactionalUpdateTaskUseCase(UpdateTaskUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "UpdateTaskUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public TaskResult updateTask(UpdateTaskCommand command) {
        return delegate.updateTask(command);
    }
}
