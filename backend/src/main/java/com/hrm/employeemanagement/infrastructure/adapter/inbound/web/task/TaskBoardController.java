package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.TaskBoardCardResult;
import com.hrm.employeemanagement.application.dto.task.TaskBoardQuery;
import com.hrm.employeemanagement.application.dto.task.TaskBoardResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskBoardUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.MoveTaskBoardStatusUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.MoveTaskBoardStatusRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tasks")
@Validated
public class TaskBoardController {

    private final GetTaskBoardUseCase getTaskBoardUseCase;
    private final MoveTaskBoardStatusUseCase moveTaskBoardStatusUseCase;

    public TaskBoardController(
            GetTaskBoardUseCase getTaskBoardUseCase,
            MoveTaskBoardStatusUseCase moveTaskBoardStatusUseCase) {
        this.getTaskBoardUseCase = Objects.requireNonNull(getTaskBoardUseCase, "GetTaskBoardUseCase must not be null");
        this.moveTaskBoardStatusUseCase = Objects.requireNonNull(moveTaskBoardStatusUseCase, "MoveTaskBoardStatusUseCase must not be null");
    }

    @GetMapping("/board")
    public ResponseEntity<ApiResponse<TaskBoardResult>> getTaskBoard(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long employeeId) {
        TaskBoardQuery query = new TaskBoardQuery(projectId, employeeId);
        TaskBoardResult result = getTaskBoardUseCase.getTaskBoard(query);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng công việc theo cột trạng thái thành công", result));
    }

    @PatchMapping("/{taskId}/board-status")
    public ResponseEntity<ApiResponse<TaskBoardCardResult>> moveTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTaskBoardStatusRequest request) {
        TaskBoardCardResult result = moveTaskBoardStatusUseCase.moveTaskStatus(request.toCommand(taskId));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái công việc trên bảng thành công", result));
    }
}
