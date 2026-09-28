package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

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
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.dto.project.RemoveProjectMemberCommand;
import com.hrm.employeemanagement.application.port.inbound.project.AddProjectMemberUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectMembersUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.RemoveProjectMemberUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.AddProjectMemberRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
@Validated
public class ProjectMemberController {

    private final GetProjectMembersUseCase getProjectMembersUseCase;
    private final AddProjectMemberUseCase addProjectMemberUseCase;
    private final RemoveProjectMemberUseCase removeProjectMemberUseCase;

    public ProjectMemberController(
            GetProjectMembersUseCase getProjectMembersUseCase,
            AddProjectMemberUseCase addProjectMemberUseCase,
            RemoveProjectMemberUseCase removeProjectMemberUseCase) {
        this.getProjectMembersUseCase = Objects.requireNonNull(getProjectMembersUseCase, "GetProjectMembersUseCase must not be null");
        this.addProjectMemberUseCase = Objects.requireNonNull(addProjectMemberUseCase, "AddProjectMemberUseCase must not be null");
        this.removeProjectMemberUseCase = Objects.requireNonNull(removeProjectMemberUseCase, "RemoveProjectMemberUseCase must not be null");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectMemberResult>>> getMembers(
            @PathVariable Long projectId) {
        List<ProjectMemberResult> members = getProjectMembersUseCase.getProjectMembers(projectId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành viên dự án thành công", members));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectMemberResult>> addMember(
            @PathVariable Long projectId,
            @Valid @RequestBody AddProjectMemberRequest request) {
        ProjectMemberResult result = addProjectMemberUseCase.addProjectMember(request.toCommand(projectId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm thành viên vào dự án thành công", result));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long employeeId) {
        removeProjectMemberUseCase.removeProjectMember(new RemoveProjectMemberCommand(projectId, employeeId));
        return ResponseEntity.ok(ApiResponse.success("Xóa thành viên khỏi dự án thành công", null));
    }
}
