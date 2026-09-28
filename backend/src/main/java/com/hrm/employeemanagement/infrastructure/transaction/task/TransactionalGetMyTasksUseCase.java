package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.MyTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyTasksUseCase;

public class TransactionalGetMyTasksUseCase implements GetMyTasksUseCase {

    private final GetMyTasksUseCase delegate;

    public TransactionalGetMyTasksUseCase(GetMyTasksUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetMyTasksUseCase delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyTaskResult> getMyTasks() {
        return delegate.getMyTasks();
    }
}

