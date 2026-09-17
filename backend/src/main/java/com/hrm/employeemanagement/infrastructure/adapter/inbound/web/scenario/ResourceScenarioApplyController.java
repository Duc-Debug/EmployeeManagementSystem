package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario;

import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.ApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.PreviewApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.RefreshScenarioBaselineUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.ApplyScenarioRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/resource-scenarios")
@Validated
public class ResourceScenarioApplyController {

    private final PreviewApplyScenarioUseCase previewApplyUseCase;
    private final ApplyScenarioUseCase applyScenarioUseCase;
    private final RefreshScenarioBaselineUseCase refreshBaselineUseCase;

    public ResourceScenarioApplyController(
            PreviewApplyScenarioUseCase previewApplyUseCase,
            ApplyScenarioUseCase applyScenarioUseCase,
            RefreshScenarioBaselineUseCase refreshBaselineUseCase
    ) {
        this.previewApplyUseCase = Objects.requireNonNull(previewApplyUseCase, "PreviewApplyScenarioUseCase must not be null");
        this.applyScenarioUseCase = Objects.requireNonNull(applyScenarioUseCase, "ApplyScenarioUseCase must not be null");
        this.refreshBaselineUseCase = Objects.requireNonNull(refreshBaselineUseCase, "RefreshScenarioBaselineUseCase must not be null");
    }

    @GetMapping("/{id}/apply-preview")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_MANAGE')")
    public ResponseEntity<ApiResponse<ApplyScenarioPreviewResult>> previewApply(
            @PathVariable("id") Long id,
            @RequestParam("targetProjectId") Long targetProjectId
    ) {
        ApplyScenarioPreviewResult result = previewApplyUseCase.previewApplyScenario(id, targetProjectId);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu đối chiếu áp dụng kịch bản thành công", result));
    }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_MANAGE')")
    public ResponseEntity<ApiResponse<ApplyScenarioResult>> applyScenario(
            @PathVariable("id") Long id,
            @Valid @RequestBody ApplyScenarioRequest request
    ) {
        ApplyScenarioCommand command = new ApplyScenarioCommand(id, request.targetProjectId(), request.note());
        ApplyScenarioResult result = applyScenarioUseCase.applyScenario(command);
        return ResponseEntity.ok(ApiResponse.success("Áp dụng kịch bản thành phân bổ thật thành công", result));
    }

    @PostMapping("/{id}/refresh-baseline")
    @PreAuthorize("hasAuthority('RESOURCE_SCENARIO_MANAGE')")
    public ResponseEntity<ApiResponse<ScenarioResult>> refreshBaseline(
            @PathVariable("id") Long id
    ) {
        ScenarioResult result = refreshBaselineUseCase.refreshScenarioBaseline(id);
        return ResponseEntity.ok(ApiResponse.success("Làm mới dữ liệu snapshot kịch bản thành công", result));
    }
}
