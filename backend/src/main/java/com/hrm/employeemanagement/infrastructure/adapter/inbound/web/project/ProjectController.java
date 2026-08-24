package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectListUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final GetProjectListUseCase getProjectListUseCase;

    public ProjectController(
            GetProjectListUseCase getProjectListUseCase
    ) {
        this.getProjectListUseCase = getProjectListUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<ProjectResult>>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<ProjectResult> projects =
                getProjectListUseCase.getProjects(
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lay danh sach du an thanh cong",
                        projects
                )
        );
    }
}
