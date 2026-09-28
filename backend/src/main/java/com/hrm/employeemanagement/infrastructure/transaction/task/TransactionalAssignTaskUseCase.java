package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.AssignTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskAssignmentResult;
import com.hrm.employeemanagement.application.port.inbound.task.AssignTaskUseCase;

public class TransactionalAssignTaskUseCase implements AssignTaskUseCase {

    private final AssignTaskUseCase delegate;

    public TransactionalAssignTaskUseCase(AssignTaskUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "AssignTaskUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public TaskAssignmentResult assignTask(AssignTaskCommand command) {
        return delegate.assignTask(command);
    }
}

