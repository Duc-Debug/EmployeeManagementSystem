package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.cascade.CascadeDelayWarningResult;
import com.hrm.employeemanagement.application.dto.task.cascade.EvaluateCascadeDelayCommand;
import com.hrm.employeemanagement.application.port.inbound.task.EvaluateCascadeDelayUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskActualEndDateUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.EvaluateCascadeDelayRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks/{taskId}")
public class CascadeDelayWarningController {

    private final EvaluateCascadeDelayUseCase evaluateUseCase;
    private final UpdateTaskActualEndDateUseCase updateUseCase;

    public CascadeDelayWarningController(
            EvaluateCascadeDelayUseCase evaluateUseCase,
            UpdateTaskActualEndDateUseCase updateUseCase) {
        this.evaluateUseCase = evaluateUseCase;
        this.updateUseCase = updateUseCase;
    }

    @PostMapping("/cascade-delay-warning/evaluate")
    public ResponseEntity<ApiResponse<CascadeDelayWarningResult>> evaluateCascadeDelay(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody EvaluateCascadeDelayRequest request) {

        EvaluateCascadeDelayCommand command = new EvaluateCascadeDelayCommand(
                projectId,
                taskId,
                request.newActualEndDate()
        );

        CascadeDelayWarningResult result = evaluateUseCase.evaluateCascadeDelay(command);
        return ResponseEntity.ok(ApiResponse.success("Đánh giá ảnh hưởng trễ dây chuyền thành công", result));
    }

    @PostMapping("/actual-end-date")
    public ResponseEntity<ApiResponse<CascadeDelayWarningResult>> updateActualEndDate(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody EvaluateCascadeDelayRequest request) {

        EvaluateCascadeDelayCommand command = new EvaluateCascadeDelayCommand(
                projectId,
                taskId,
                request.newActualEndDate()
        );

        CascadeDelayWarningResult result = updateUseCase.updateActualEndDate(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ngày kết thúc thực tế và đánh giá trễ dây chuyền thành công", result));
    }
}
