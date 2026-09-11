package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleUsageResult;
import com.hrm.employeemanagement.application.port.inbound.project.ActivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CheckProjectRoleUsageUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.DeactivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectRolesUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectRoleUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.CreateProjectRoleRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.ProjectRoleResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.UpdateProjectRoleRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/project-roles")
public class ProjectRoleController {

    private final GetProjectRolesUseCase getProjectRolesUseCase;
    private final CreateProjectRoleUseCase createProjectRoleUseCase;
    private final UpdateProjectRoleUseCase updateProjectRoleUseCase;
    private final DeactivateProjectRoleUseCase deactivateProjectRoleUseCase;
    private final ActivateProjectRoleUseCase activateProjectRoleUseCase;
    private final CheckProjectRoleUsageUseCase checkProjectRoleUsageUseCase;

    public ProjectRoleController(GetProjectRolesUseCase getProjectRolesUseCase) {
        this(getProjectRolesUseCase, null, null, null, null, null);
    }

    @Autowired
    public ProjectRoleController(
            GetProjectRolesUseCase getProjectRolesUseCase,
            CreateProjectRoleUseCase createProjectRoleUseCase,
            UpdateProjectRoleUseCase updateProjectRoleUseCase,
            DeactivateProjectRoleUseCase deactivateProjectRoleUseCase,
            ActivateProjectRoleUseCase activateProjectRoleUseCase,
            CheckProjectRoleUsageUseCase checkProjectRoleUsageUseCase) {
        this.getProjectRolesUseCase = getProjectRolesUseCase;
        this.createProjectRoleUseCase = createProjectRoleUseCase;
        this.updateProjectRoleUseCase = updateProjectRoleUseCase;
        this.deactivateProjectRoleUseCase = deactivateProjectRoleUseCase;
        this.activateProjectRoleUseCase = activateProjectRoleUseCase;
        this.checkProjectRoleUsageUseCase = checkProjectRoleUsageUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectRoleResponse>>> getProjectRoles(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        List<ProjectRoleResult> roleResults = includeInactive
                ? getProjectRolesUseCase.getProjectRoles(true)
                : getProjectRolesUseCase.getProjectRoles();
        List<ProjectRoleResponse> roles = roleResults.stream()
                .map(ProjectRoleResponse::fromResult)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục vai trò dự án thành công", roles));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectRoleResponse>> createProjectRole(
            @Valid @RequestBody CreateProjectRoleRequest request) {
        ProjectRoleResult result = createProjectRoleUseCase.createProjectRole(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo vai trò chuyên môn thành công", ProjectRoleResponse.fromResult(result)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectRoleResponse>> updateProjectRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRoleRequest request) {
        ProjectRoleResult result = updateProjectRoleUseCase.updateProjectRole(request.toCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò chuyên môn thành công", ProjectRoleResponse.fromResult(result)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProjectRoleResponse>> deactivateProjectRole(@PathVariable Long id) {
        ProjectRoleResult result = deactivateProjectRoleUseCase.deactivateProjectRole(id);
        return ResponseEntity.ok(ApiResponse.success("Ngừng sử dụng vai trò chuyên môn thành công", ProjectRoleResponse.fromResult(result)));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProjectRoleResponse>> activateProjectRole(@PathVariable Long id) {
        ProjectRoleResult result = activateProjectRoleUseCase.activateProjectRole(id);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt lại vai trò chuyên môn thành công", ProjectRoleResponse.fromResult(result)));
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<ApiResponse<ProjectRoleUsageResult>> checkUsage(@PathVariable Long id) {
        ProjectRoleUsageResult result = checkProjectRoleUsageUseCase.checkUsage(id);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra việc sử dụng vai trò thành công", result));
    }
}
