package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.dependency.CreateTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.DeleteTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyGraphResult;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyResult;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.DeleteTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskDependenciesUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.CreateTaskDependencyRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks/dependencies")
public class TaskDependencyController {

    private final CreateTaskDependencyUseCase createUseCase;
    private final DeleteTaskDependencyUseCase deleteUseCase;
    private final GetTaskDependenciesUseCase getUseCase;

    public TaskDependencyController(
            CreateTaskDependencyUseCase createUseCase,
            DeleteTaskDependencyUseCase deleteUseCase,
            GetTaskDependenciesUseCase getUseCase) {
        this.createUseCase = createUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskDependencyResult>> createDependency(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskDependencyRequest request) {

        CreateTaskDependencyCommand command = new CreateTaskDependencyCommand(
                projectId,
                request.predecessorId(),
                request.successorId(),
                request.dependencyType(),
                request.lagDays()
        );

        TaskDependencyResult result = createUseCase.createDependency(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Khai báo phụ thuộc công việc thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TaskDependencyGraphResult>> getTaskDependencies(
            @PathVariable Long projectId) {

        TaskDependencyGraphResult result = getUseCase.getTaskDependencies(projectId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<ApiResponse<Void>> deleteDependency(
            @PathVariable Long projectId,
            @PathVariable Long dependencyId) {

        DeleteTaskDependencyCommand command = new DeleteTaskDependencyCommand(projectId, dependencyId);
        deleteUseCase.deleteDependency(command);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa phụ thuộc công việc thành công"));
    }
}
