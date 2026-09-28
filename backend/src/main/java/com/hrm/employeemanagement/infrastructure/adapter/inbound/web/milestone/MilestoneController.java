package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.port.inbound.milestone.CreateMilestoneUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.DeleteMilestoneUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.GetProjectMilestonesUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.UpdateMilestoneUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone.dto.CreateMilestoneRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone.dto.MilestoneResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone.dto.UpdateMilestoneRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
@Validated
public class MilestoneController {

    private final CreateMilestoneUseCase createMilestoneUseCase;
    private final UpdateMilestoneUseCase updateMilestoneUseCase;
    private final GetProjectMilestonesUseCase getProjectMilestonesUseCase;
    private final DeleteMilestoneUseCase deleteMilestoneUseCase;

    public MilestoneController(
            CreateMilestoneUseCase createMilestoneUseCase,
            UpdateMilestoneUseCase updateMilestoneUseCase,
            GetProjectMilestonesUseCase getProjectMilestonesUseCase,
            DeleteMilestoneUseCase deleteMilestoneUseCase) {
        this.createMilestoneUseCase = Objects.requireNonNull(createMilestoneUseCase, "CreateMilestoneUseCase must not be null");
        this.updateMilestoneUseCase = Objects.requireNonNull(updateMilestoneUseCase, "UpdateMilestoneUseCase must not be null");
        this.getProjectMilestonesUseCase = Objects.requireNonNull(getProjectMilestonesUseCase, "GetProjectMilestonesUseCase must not be null");
        this.deleteMilestoneUseCase = Objects.requireNonNull(deleteMilestoneUseCase, "DeleteMilestoneUseCase must not be null");
    }

    @PostMapping("/{projectId}/milestones")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateMilestoneRequest request) {
        MilestoneResult result = createMilestoneUseCase.createMilestone(request.toCommand(projectId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khai báo mốc tiến độ thành công", MilestoneResponse.from(result)));
    }

    @GetMapping("/{projectId}/milestones")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getProjectMilestones(
            @PathVariable Long projectId) {
        List<MilestoneResult> results = getProjectMilestonesUseCase.getProjectMilestones(projectId);
        List<MilestoneResponse> responseList = results.stream()
                .map(MilestoneResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mốc tiến độ thành công", responseList));
    }

    @RequestMapping(
            value = "/{projectId}/milestones/{milestoneId}",
            method = {RequestMethod.PUT, RequestMethod.PATCH}
    )
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody UpdateMilestoneRequest request) {
        MilestoneResult result = updateMilestoneUseCase.updateMilestone(request.toCommand(projectId, milestoneId));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mốc tiến độ thành công", MilestoneResponse.from(result)));
    }

    @DeleteMapping("/{projectId}/milestones/{milestoneId}")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId) {
        deleteMilestoneUseCase.deleteMilestone(projectId, milestoneId);
        return ResponseEntity.ok(ApiResponse.success("Xóa mốc tiến độ thành công", null));
    }
}
