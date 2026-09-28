package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult;

public interface PreviewApplyScenarioUseCase {
    ApplyScenarioPreviewResult previewApplyScenario(Long scenarioId, Long targetProjectId);
}

