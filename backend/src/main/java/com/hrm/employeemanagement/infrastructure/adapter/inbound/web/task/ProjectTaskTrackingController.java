package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.tracking.ProjectTaskTrackingResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingQuery;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectTaskTrackingUseCase;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * Controller cho chức năng Bảng theo dõi công việc của dự án (NCL-04-CN-003).
 * Dành cho Quản lý dự án nắm toàn bộ công việc, tiến độ, thời hạn và phát hiện công việc chậm trễ.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Validated
public class ProjectTaskTrackingController {

    private final GetProjectTaskTrackingUseCase getProjectTaskTrackingUseCase;

    public ProjectTaskTrackingController(GetProjectTaskTrackingUseCase getProjectTaskTrackingUseCase) {
        this.getProjectTaskTrackingUseCase = Objects.requireNonNull(getProjectTaskTrackingUseCase, "GetProjectTaskTrackingUseCase must not be null");
    }

    @GetMapping("/{projectId}/task-tracking")
    public ResponseEntity<ApiResponse<ProjectTaskTrackingResult>> getTaskTracking(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Boolean overdueOnly) {
        TaskTrackingQuery query = new TaskTrackingQuery(projectId, employeeId, status, overdueOnly);
        ProjectTaskTrackingResult result = getProjectTaskTrackingUseCase.getTaskTracking(query);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng theo dõi công việc dự án thành công", result));
    }
}
