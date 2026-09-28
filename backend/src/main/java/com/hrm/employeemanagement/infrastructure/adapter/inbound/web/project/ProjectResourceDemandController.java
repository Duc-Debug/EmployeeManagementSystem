package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.project.DeleteProjectResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.EstimateResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectResourceDemandUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.EstimateResourceDemandRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/resource-demands")
@Validated
public class ProjectResourceDemandController {

    private final EstimateResourceDemandUseCase estimateResourceDemandUseCase;
    private final GetProjectResourceDemandUseCase getProjectResourceDemandUseCase;
    private final DeleteProjectResourceDemandUseCase deleteProjectResourceDemandUseCase;

    public ProjectResourceDemandController(
            EstimateResourceDemandUseCase estimateResourceDemandUseCase,
            GetProjectResourceDemandUseCase getProjectResourceDemandUseCase,
            DeleteProjectResourceDemandUseCase deleteProjectResourceDemandUseCase) {
        this.estimateResourceDemandUseCase = Objects.requireNonNull(
                estimateResourceDemandUseCase, "EstimateResourceDemandUseCase must not be null");
        this.getProjectResourceDemandUseCase = Objects.requireNonNull(
                getProjectResourceDemandUseCase, "GetProjectResourceDemandUseCase must not be null");
        this.deleteProjectResourceDemandUseCase = Objects.requireNonNull(
                deleteProjectResourceDemandUseCase, "DeleteProjectResourceDemandUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResourceDemandSummaryResult>> estimateResourceDemand(
            @PathVariable Long projectId,
            @Valid @RequestBody EstimateResourceDemandRequest request) {

        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                projectId,
                request.roleId(),
                request.hoursPerWeek());

        ProjectResourceDemandSummaryResult result = estimateResourceDemandUseCase.estimateDemand(command);

        String message = result.exceedsEstimatedHours()
                ? "Ước lượng nhu cầu nhân sự thành công (Có cảnh báo)"
                : "Ước lượng nhu cầu nhân sự thành công";

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProjectResourceDemandSummaryResult>> getProjectResourceDemands(
            @PathVariable Long projectId) {

        ProjectResourceDemandSummaryResult result = getProjectResourceDemandUseCase.getProjectResourceDemands(projectId);

        return ResponseEntity.ok(ApiResponse.success("Lấy bảng nhu cầu nhân sự thành công", result));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<ProjectResourceDemandSummaryResult>> deleteResourceDemand(
            @PathVariable Long projectId,
            @PathVariable Long roleId) {

        ProjectResourceDemandSummaryResult result = deleteProjectResourceDemandUseCase.deleteDemand(projectId, roleId);

        return ResponseEntity.ok(ApiResponse.success("Xóa ước lượng nhu cầu nhân sự của vai trò thành công", result));
    }
}