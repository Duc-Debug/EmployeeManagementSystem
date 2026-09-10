package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.project.CloseProjectCommand;
import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.ReopenProjectCommand;
import com.hrm.employeemanagement.application.dto.project.UpdateProjectCommand;
import com.hrm.employeemanagement.application.dto.projecttemplate.CreateProjectFromTemplateCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.project.CloseProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectDetailUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectListUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.ReopenProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.projecttemplate.CreateProjectFromTemplateUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.CloseProjectRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.CreateProjectFromTemplateRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.CreateProjectRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.ReopenProjectRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.UpdateProjectRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/projects")
@Validated
public class ProjectController {

        private final GetProjectListUseCase getProjectListUseCase;
        private final GetProjectDetailUseCase getProjectDetailUseCase;
        private final CreateProjectUseCase createProjectUseCase;
        private final UpdateProjectUseCase updateProjectUseCase;
        private final CreateProjectFromTemplateUseCase createProjectFromTemplateUseCase;
        private final CloseProjectUseCase closeProjectUseCase;
        private final ReopenProjectUseCase reopenProjectUseCase;

        public ProjectController(
                        GetProjectListUseCase getProjectListUseCase,
                        GetProjectDetailUseCase getProjectDetailUseCase,
                        CreateProjectUseCase createProjectUseCase,
                        UpdateProjectUseCase updateProjectUseCase,
                        CreateProjectFromTemplateUseCase createProjectFromTemplateUseCase,
                        CloseProjectUseCase closeProjectUseCase,
                        ReopenProjectUseCase reopenProjectUseCase) {
                this.getProjectListUseCase = getProjectListUseCase;
                this.getProjectDetailUseCase = getProjectDetailUseCase;
                this.createProjectUseCase = createProjectUseCase;
                this.updateProjectUseCase = updateProjectUseCase;
                this.createProjectFromTemplateUseCase = createProjectFromTemplateUseCase;
                this.closeProjectUseCase = closeProjectUseCase;
                this.reopenProjectUseCase = reopenProjectUseCase;
        }

        @GetMapping
        public ResponseEntity<ApiResponse<PageResult<ProjectResult>>> getProjects(
                        @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang phải lớn hơn hoặc bằng 0") int page,
                        @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100") @Max(value = 100, message = "Kích thước trang tối đa là 100") int size) {
                PageResult<ProjectResult> projects = getProjectListUseCase.getProjects(
                                page,
                                size);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Lay danh sach du an thanh cong",
                                                projects));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<ProjectResult>> getProjectById(
                        @PathVariable Long id) {
                ProjectResult project = getProjectDetailUseCase.getProjectById(id);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Lay thong tin du an thanh cong",
                                                project));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<ProjectResult>> createProject(
                        @Valid @RequestBody CreateProjectRequest request) {
                CreateProjectCommand command = new CreateProjectCommand(
                                request.projectName(),
                                request.orgUnitId(),
                                request.managerId(),
                                request.startDate(),
                                request.endDate(),
                                request.estimatedHours(),
                                request.description());
                ProjectResult result = createProjectUseCase.createProject(command);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Tạo dự án thành công", result));
        }

        @PostMapping("/from-template")
        public ResponseEntity<ApiResponse<ProjectResult>> createProjectFromTemplate(
                        @Valid @RequestBody CreateProjectFromTemplateRequest request) {
                CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                                request.templateId(),
                                request.projectName(),
                                request.orgUnitId(),
                                request.managerId(),
                                request.startDate(),
                                request.endDate(),
                                request.description());
                ProjectResult result = createProjectFromTemplateUseCase.createProjectFromTemplate(command);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Tạo dự án từ mẫu thành công", result));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<ProjectResult>> updateProject(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateProjectRequest request) {
                UpdateProjectCommand command = new UpdateProjectCommand(
                                id,
                                request.projectName(),
                                request.managerId(),
                                request.startDate(),
                                request.endDate(),
                                request.estimatedHours(),
                                request.description());
                ProjectResult result = updateProjectUseCase.updateProject(command);
                return ResponseEntity.ok(ApiResponse.success("Cập nhật dự án thành công", result));
        }

        @PostMapping("/{id}/close")
        public ResponseEntity<ApiResponse<ProjectResult>> closeProject(
                        @PathVariable Long id,
                        @Valid @RequestBody(required = false) CloseProjectRequest request) {
                String reason = request != null ? request.closureReason() : null;
                CloseProjectCommand command = new CloseProjectCommand(id, reason);
                ProjectResult result = closeProjectUseCase.closeProject(command);
                return ResponseEntity.ok(ApiResponse.success("Đóng dự án thành công", result));
        }

        @PostMapping("/{id}/reopen")
        public ResponseEntity<ApiResponse<ProjectResult>> reopenProject(
                        @PathVariable Long id,
                        @Valid @RequestBody ReopenProjectRequest request) {
                ReopenProjectCommand command = new ReopenProjectCommand(id, request.reopenReason());
                ProjectResult result = reopenProjectUseCase.reopenProject(command);
                return ResponseEntity.ok(ApiResponse.success("Mở lại dự án thành công", result));
        }
}
