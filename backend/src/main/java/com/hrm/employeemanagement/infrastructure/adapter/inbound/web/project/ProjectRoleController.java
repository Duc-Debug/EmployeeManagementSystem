package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.port.inbound.project.GetProjectRolesUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.ProjectRoleResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/project-roles")
public class ProjectRoleController {

    private final GetProjectRolesUseCase getProjectRolesUseCase;

    public ProjectRoleController(GetProjectRolesUseCase getProjectRolesUseCase) {
        this.getProjectRolesUseCase = getProjectRolesUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectRoleResponse>>> getProjectRoles() {
        List<ProjectRoleResponse> roles = getProjectRolesUseCase.getProjectRoles().stream()
                .map(ProjectRoleResponse::fromResult)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục vai trò dự án thành công", roles));
    }
}
