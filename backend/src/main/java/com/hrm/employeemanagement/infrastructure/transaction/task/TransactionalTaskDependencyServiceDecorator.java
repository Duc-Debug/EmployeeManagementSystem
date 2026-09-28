package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.dependency.CreateTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.DeleteTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyGraphResult;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyResult;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.DeleteTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskDependenciesUseCase;
import com.hrm.employeemanagement.application.service.task.TaskDependencyService;

public class TransactionalTaskDependencyServiceDecorator implements
        CreateTaskDependencyUseCase,
        DeleteTaskDependencyUseCase,
        GetTaskDependenciesUseCase {

    private final TaskDependencyService delegate;

    public TransactionalTaskDependencyServiceDecorator(TaskDependencyService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "TaskDependencyService must not be null");
    }

    @Override
    @Transactional
    public TaskDependencyResult createDependency(CreateTaskDependencyCommand command) {
        return delegate.createDependency(command);
    }

    @Override
    @Transactional
    public void deleteDependency(DeleteTaskDependencyCommand command) {
        delegate.deleteDependency(command);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDependencyGraphResult getTaskDependencies(Long projectId) {
        return delegate.getTaskDependencies(projectId);
    }
}
