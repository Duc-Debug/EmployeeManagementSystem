package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.SetTaskBudgetCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBudgetResult;
import com.hrm.employeemanagement.application.port.inbound.task.SetTaskBudgetUseCase;

public class TransactionalSetTaskBudgetUseCase implements SetTaskBudgetUseCase {

    private final SetTaskBudgetUseCase delegate;

    public TransactionalSetTaskBudgetUseCase(SetTaskBudgetUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "SetTaskBudgetUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public TaskBudgetResult setTaskBudget(SetTaskBudgetCommand command) {
        return delegate.setTaskBudget(command);
    }
}
