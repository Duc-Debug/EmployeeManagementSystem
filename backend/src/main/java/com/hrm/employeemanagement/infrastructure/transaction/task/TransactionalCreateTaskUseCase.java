package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.CreateTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskUseCase;

public class TransactionalCreateTaskUseCase implements CreateTaskUseCase {

    private final CreateTaskUseCase delegate;

    public TransactionalCreateTaskUseCase(CreateTaskUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CreateTaskUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public TaskResult createTask(CreateTaskCommand command) {
        return delegate.createTask(command);
    }
}
