package com.hrm.employeemanagement.infrastructure.transaction.allocation.template;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateResult;
import com.hrm.employeemanagement.application.dto.allocation.template.CreateRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.PreviewRoleAllocationResult;
import com.hrm.employeemanagement.application.dto.allocation.template.ProjectRoleStructureItem;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateDetailResult;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.ApplyRoleAllocationTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.CreateRoleAllocationTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.GetProjectRoleAllocationStructureUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.GetRoleAllocationTemplatesUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.PreviewRoleAllocationSuggestionUseCase;
import com.hrm.employeemanagement.application.service.allocation.template.RoleAllocationTemplateService;

public class TransactionalRoleAllocationTemplateServiceDecorator implements
        CreateRoleAllocationTemplateUseCase,
        GetRoleAllocationTemplatesUseCase,
        GetProjectRoleAllocationStructureUseCase,
        PreviewRoleAllocationSuggestionUseCase,
        ApplyRoleAllocationTemplateUseCase {

    private final RoleAllocationTemplateService delegate;

    public TransactionalRoleAllocationTemplateServiceDecorator(RoleAllocationTemplateService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "RoleAllocationTemplateService must not be null");
    }

    @Override
    @Transactional
    public RoleAllocationTemplateDetailResult createTemplate(CreateRoleAllocationTemplateCommand command) {
        return delegate.createTemplate(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleAllocationTemplateSummaryResult> getAllTemplates() {
        return delegate.getAllTemplates();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleAllocationTemplateDetailResult getTemplateById(Long id) {
        return delegate.getTemplateById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectRoleStructureItem> getStructureFromProject(Long projectId) {
        return delegate.getStructureFromProject(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewRoleAllocationResult previewSuggestion(Long templateId, Long targetProjectId) {
        return delegate.previewSuggestion(templateId, targetProjectId);
    }

    @Override
    @Transactional
    public ApplyRoleAllocationTemplateResult applyTemplate(ApplyRoleAllocationTemplateCommand command) {
        return delegate.applyTemplate(command);
    }
}

