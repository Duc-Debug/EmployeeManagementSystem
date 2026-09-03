package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectDetailUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectListUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.CreateProjectRequest;
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

        public ProjectController(
                        GetProjectListUseCase getProjectListUseCase,
                        GetProjectDetailUseCase getProjectDetailUseCase, CreateProjectUseCase createProjectUseCase) {
                this.getProjectListUseCase = getProjectListUseCase;
                this.getProjectDetailUseCase = getProjectDetailUseCase;
                this.createProjectUseCase = createProjectUseCase;
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
                        @org.springframework.web.bind.annotation.PathVariable Long id) {
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
}
