package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.TaskProgressResult;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskProgressUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.UpdateTaskProgressRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

/**
 * Controller cho chức năng Cập nhật tiến độ công việc (NCL-04-CN-002).
 * Dành cho nhân viên chuyên môn (người được giao việc) cập nhật trạng thái công việc.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Validated
public class TaskProgressController {

    private final UpdateTaskProgressUseCase updateTaskProgressUseCase;

    public TaskProgressController(UpdateTaskProgressUseCase updateTaskProgressUseCase) {
        this.updateTaskProgressUseCase = Objects.requireNonNull(updateTaskProgressUseCase, "UpdateTaskProgressUseCase must not be null");
    }

    @PatchMapping("/{taskId}/progress")
    public ResponseEntity<ApiResponse<TaskProgressResult>> updateProgress(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskProgressRequest request) {
        TaskProgressResult result = updateTaskProgressUseCase.updateProgress(request.toCommand(taskId));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tiến độ công việc thành công", result));
    }
}
