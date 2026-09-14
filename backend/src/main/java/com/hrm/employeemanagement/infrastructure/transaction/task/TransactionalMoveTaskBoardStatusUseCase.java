package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.MoveTaskBoardStatusCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBoardCardResult;
import com.hrm.employeemanagement.application.port.inbound.task.MoveTaskBoardStatusUseCase;

public class TransactionalMoveTaskBoardStatusUseCase implements MoveTaskBoardStatusUseCase {

    private final MoveTaskBoardStatusUseCase delegate;

    public TransactionalMoveTaskBoardStatusUseCase(MoveTaskBoardStatusUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "MoveTaskBoardStatusUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public TaskBoardCardResult moveTaskStatus(MoveTaskBoardStatusCommand command) {
        return delegate.moveTaskStatus(command);
    }
}
