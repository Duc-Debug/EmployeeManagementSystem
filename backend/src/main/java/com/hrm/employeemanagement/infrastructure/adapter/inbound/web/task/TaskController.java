package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsResult;
import com.hrm.employeemanagement.application.dto.task.TaskBudgetResult;
import com.hrm.employeemanagement.application.dto.task.TaskNodeResult;
import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.CloneProjectWbsUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectWbsUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.SetTaskBudgetUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.CloneProjectWbsRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.CreateTaskRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.SetTaskBudgetRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.UpdateTaskRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
@Validated
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final GetProjectWbsUseCase getProjectWbsUseCase;
    private final SetTaskBudgetUseCase setTaskBudgetUseCase;
    private final CloneProjectWbsUseCase cloneProjectWbsUseCase;

    public TaskController(
            CreateTaskUseCase createTaskUseCase,
            UpdateTaskUseCase updateTaskUseCase,
            GetProjectWbsUseCase getProjectWbsUseCase,
            SetTaskBudgetUseCase setTaskBudgetUseCase,
            CloneProjectWbsUseCase cloneProjectWbsUseCase) {
        this.createTaskUseCase = Objects.requireNonNull(createTaskUseCase, "CreateTaskUseCase must not be null");
        this.updateTaskUseCase = Objects.requireNonNull(updateTaskUseCase, "UpdateTaskUseCase must not be null");
        this.getProjectWbsUseCase = Objects.requireNonNull(getProjectWbsUseCase, "GetProjectWbsUseCase must not be null");
        this.setTaskBudgetUseCase = Objects.requireNonNull(setTaskBudgetUseCase, "SetTaskBudgetUseCase must not be null");
        this.cloneProjectWbsUseCase = Objects.requireNonNull(cloneProjectWbsUseCase, "CloneProjectWbsUseCase must not be null");
    }

    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<ApiResponse<TaskResult>> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResult result = createTaskUseCase.createTask(request.toCommand(projectId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo hạng mục/công việc thành công", result));
    }

    @RequestMapping(value = "/{projectId}/tasks/{taskId}", method = {org.springframework.web.bind.annotation.RequestMethod.PUT, org.springframework.web.bind.annotation.RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<TaskResult>> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResult result = updateTaskUseCase.updateTask(request.toCommand(projectId, taskId));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hạng mục/công việc thành công", result));
    }

    @GetMapping("/{projectId}/wbs")
    public ResponseEntity<ApiResponse<List<TaskNodeResult>>> getProjectWbs(
            @PathVariable Long projectId) {
        List<TaskNodeResult> wbs = getProjectWbsUseCase.getProjectWbs(projectId);
        return ResponseEntity.ok(ApiResponse.success("Lấy cây công việc WBS thành công", wbs));
    }

    @PatchMapping("/{projectId}/tasks/{taskId}/budget")
    public ResponseEntity<ApiResponse<TaskBudgetResult>> setTaskBudget(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody SetTaskBudgetRequest request) {
        TaskBudgetResult result = setTaskBudgetUseCase.setTaskBudget(request.toCommand(projectId, taskId));
        return ResponseEntity.ok(ApiResponse.success("Đặt ngân sách giờ công thành công", result));
    }

    @PostMapping("/{projectId}/wbs/clone")
    public ResponseEntity<ApiResponse<CloneProjectWbsResult>> cloneWbs(
            @PathVariable Long projectId,
            @Valid @RequestBody CloneProjectWbsRequest request) {
        CloneProjectWbsResult result = cloneProjectWbsUseCase.cloneWbs(request.toCommand(projectId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nhân bản cây công việc thành công", result));
    }
}
