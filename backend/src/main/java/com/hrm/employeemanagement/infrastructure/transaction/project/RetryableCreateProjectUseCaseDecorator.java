package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;

public class RetryableCreateProjectUseCaseDecorator implements CreateProjectUseCase {

    private final CreateProjectUseCase transactionalDelegate;
    private final int maxRetries;

    public RetryableCreateProjectUseCaseDecorator(CreateProjectUseCase transactionalDelegate) {
        this(transactionalDelegate, 3);
    }

    public RetryableCreateProjectUseCaseDecorator(CreateProjectUseCase transactionalDelegate, int maxRetries) {
        this.transactionalDelegate = Objects.requireNonNull(transactionalDelegate, "CreateProjectUseCase delegate must not be null");
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be at least 1");
        }
        this.maxRetries = maxRetries;
    }

    @Override
    public ProjectResult createProject(CreateProjectCommand command) {
        DuplicateProjectCodeException lastDuplicateException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return transactionalDelegate.createProject(command);
            } catch (DuplicateProjectCodeException ex) {
                lastDuplicateException = ex;
            }
        }
        throw lastDuplicateException != null ? lastDuplicateException
                : new DuplicateProjectCodeException("Không thể khởi tạo mã dự án duy nhất sau nhiều lần thử");
    }
}