package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.TaskBoardQuery;
import com.hrm.employeemanagement.application.dto.task.TaskBoardResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskBoardUseCase;

public class TransactionalGetTaskBoardUseCase implements GetTaskBoardUseCase {

    private final GetTaskBoardUseCase delegate;

    public TransactionalGetTaskBoardUseCase(GetTaskBoardUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetTaskBoardUseCase delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public TaskBoardResult getTaskBoard(TaskBoardQuery query) {
        return delegate.getTaskBoard(query);
    }
}
