package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.TaskNodeResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectWbsUseCase;

public class TransactionalGetProjectWbsUseCase implements GetProjectWbsUseCase {

    private final GetProjectWbsUseCase delegate;

    public TransactionalGetProjectWbsUseCase(GetProjectWbsUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetProjectWbsUseCase delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskNodeResult> getProjectWbs(Long projectId) {
        return delegate.getProjectWbs(projectId);
    }
}
