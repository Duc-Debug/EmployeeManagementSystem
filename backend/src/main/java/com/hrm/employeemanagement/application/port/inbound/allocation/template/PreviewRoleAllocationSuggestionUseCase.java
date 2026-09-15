package com.hrm.employeemanagement.application.port.inbound.allocation.template;

import com.hrm.employeemanagement.application.dto.allocation.template.PreviewRoleAllocationResult;

public interface PreviewRoleAllocationSuggestionUseCase {
    PreviewRoleAllocationResult previewSuggestion(Long templateId, Long targetProjectId);
}

