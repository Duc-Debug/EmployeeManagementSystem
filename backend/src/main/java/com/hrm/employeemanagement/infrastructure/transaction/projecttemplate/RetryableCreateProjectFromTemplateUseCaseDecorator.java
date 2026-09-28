package com.hrm.employeemanagement.infrastructure.transaction.projecttemplate;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.CreateProjectFromTemplateCommand;
import com.hrm.employeemanagement.application.port.inbound.projecttemplate.CreateProjectFromTemplateUseCase;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;

public class RetryableCreateProjectFromTemplateUseCaseDecorator implements CreateProjectFromTemplateUseCase {

    private final CreateProjectFromTemplateUseCase transactionalDelegate;
    private final int maxRetries;

    public RetryableCreateProjectFromTemplateUseCaseDecorator(CreateProjectFromTemplateUseCase transactionalDelegate) {
        this(transactionalDelegate, 3);
    }

    public RetryableCreateProjectFromTemplateUseCaseDecorator(CreateProjectFromTemplateUseCase transactionalDelegate, int maxRetries) {
        this.transactionalDelegate = Objects.requireNonNull(transactionalDelegate, "CreateProjectFromTemplateUseCase delegate must not be null");
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be at least 1");
        }
        this.maxRetries = maxRetries;
    }

    @Override
    public ProjectResult createProjectFromTemplate(CreateProjectFromTemplateCommand command) {
        DuplicateProjectCodeException lastDuplicateException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return transactionalDelegate.createProjectFromTemplate(command);
            } catch (DuplicateProjectCodeException ex) {
                lastDuplicateException = ex;
            }
        }
        throw lastDuplicateException != null ? lastDuplicateException
                : new DuplicateProjectCodeException("Không thể khởi tạo mã dự án duy nhất sau nhiều lần thử");
    }
}
