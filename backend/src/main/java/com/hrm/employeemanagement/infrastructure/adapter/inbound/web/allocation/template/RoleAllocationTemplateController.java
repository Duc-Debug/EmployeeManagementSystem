package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.template;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.template.dto.ApplyRoleAllocationTemplateRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.template.dto.CreateRoleAllocationTemplateRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/role-allocation-templates")
@Validated
public class RoleAllocationTemplateController {

    private final CreateRoleAllocationTemplateUseCase createUseCase;
    private final GetRoleAllocationTemplatesUseCase getTemplatesUseCase;
    private final GetProjectRoleAllocationStructureUseCase getStructureUseCase;
    private final PreviewRoleAllocationSuggestionUseCase previewUseCase;
    private final ApplyRoleAllocationTemplateUseCase applyUseCase;

    public RoleAllocationTemplateController(
            CreateRoleAllocationTemplateUseCase createUseCase,
            GetRoleAllocationTemplatesUseCase getTemplatesUseCase,
            GetProjectRoleAllocationStructureUseCase getStructureUseCase,
            PreviewRoleAllocationSuggestionUseCase previewUseCase,
            ApplyRoleAllocationTemplateUseCase applyUseCase
    ) {
        this.createUseCase = Objects.requireNonNull(createUseCase);
        this.getTemplatesUseCase = Objects.requireNonNull(getTemplatesUseCase);
        this.getStructureUseCase = Objects.requireNonNull(getStructureUseCase);
        this.previewUseCase = Objects.requireNonNull(previewUseCase);
        this.applyUseCase = Objects.requireNonNull(applyUseCase);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleAllocationTemplateDetailResult>> createTemplate(
            @Valid @RequestBody CreateRoleAllocationTemplateRequest request
    ) {
        List<CreateRoleAllocationTemplateCommand.ItemCommand> itemCommands = request.items().stream()
                .map(i -> new CreateRoleAllocationTemplateCommand.ItemCommand(i.roleId(), i.hoursPerWeek()))
                .collect(Collectors.toList());

        CreateRoleAllocationTemplateCommand command = new CreateRoleAllocationTemplateCommand(
                request.templateCode(),
                request.name(),
                request.description(),
                request.sourceProjectId(),
                itemCommands
        );

        RoleAllocationTemplateDetailResult result = createUseCase.createTemplate(command);
        return ResponseEntity.created(URI.create("/api/v1/role-allocation-templates/" + result.id()))
                .body(ApiResponse.success("Lưu mẫu phân bổ theo vai trò thành công", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleAllocationTemplateSummaryResult>>> getAllTemplates() {
        List<RoleAllocationTemplateSummaryResult> result = getTemplatesUseCase.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mẫu phân bổ thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleAllocationTemplateDetailResult>> getTemplateById(@PathVariable Long id) {
        RoleAllocationTemplateDetailResult result = getTemplatesUseCase.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết mẫu phân bổ thành công", result));
    }

    @GetMapping("/extract-from-project/{projectId}")
    public ResponseEntity<ApiResponse<List<ProjectRoleStructureItem>>> extractFromProject(@PathVariable Long projectId) {
        List<ProjectRoleStructureItem> result = getStructureUseCase.getStructureFromProject(projectId);
        return ResponseEntity.ok(ApiResponse.success("Trích xuất cơ cấu vai trò từ dự án thành công", result));
    }

    @PostMapping("/{id}/preview-apply/{targetProjectId}")
    public ResponseEntity<ApiResponse<PreviewRoleAllocationResult>> previewApply(
            @PathVariable Long id,
            @PathVariable Long targetProjectId
    ) {
        PreviewRoleAllocationResult result = previewUseCase.previewSuggestion(id, targetProjectId);
        String message = result.hasUnassignedRoles()
                ? "Gợi ý phân bổ nhân sự cho dự án (Có vai trò chưa tìm được người rảnh)"
                : "Gợi ý phân bổ nhân sự cho dự án thành công";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @PostMapping("/{id}/confirm-apply/{targetProjectId}")
    public ResponseEntity<ApiResponse<ApplyRoleAllocationTemplateResult>> confirmApply(
            @PathVariable Long id,
            @PathVariable Long targetProjectId,
            @Valid @RequestBody ApplyRoleAllocationTemplateRequest request
    ) {
        List<ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand> assignmentCommands = request.assignments().stream()
                .map(a -> new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(a.roleId(), a.employeeId(), a.hoursPerWeek()))
                .collect(Collectors.toList());

        ApplyRoleAllocationTemplateCommand command = new ApplyRoleAllocationTemplateCommand(
                id,
                targetProjectId,
                assignmentCommands
        );

        ApplyRoleAllocationTemplateResult result = applyUseCase.applyTemplate(command);
        return ResponseEntity.ok(ApiResponse.success(result.message(), result));
    }
}

