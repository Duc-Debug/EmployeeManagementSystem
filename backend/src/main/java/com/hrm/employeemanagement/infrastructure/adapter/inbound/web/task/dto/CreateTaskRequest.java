package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import java.math.BigDecimal;

import com.hrm.employeemanagement.application.dto.task.CreateTaskCommand;
import com.hrm.employeemanagement.domain.task.TaskType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        Long parentId,
        @Size(max = 50, message = "Mã công việc không được vượt quá 50 ký tự")
        String taskCode,
        @NotBlank(message = "Tên hạng mục / công việc không được để trống")
        @Size(max = 255, message = "Tên hạng mục / công việc không được vượt quá 255 ký tự")
        String name,
        @Size(max = 2000, message = "Mô tả công việc không được vượt quá 2000 ký tự")
        String description,
        TaskType taskType,
        Long assigneeId,
        @DecimalMin(value = "0.0", inclusive = true, message = "Thời gian dự kiến không được nhỏ hơn 0")
        @Digits(integer = 8, fraction = 2, message = "Thời gian dự kiến chỉ được có tối đa 8 chữ số phần nguyên và 2 chữ số phần thập phân")
        BigDecimal estimatedHours,
        Integer sortOrder) {

    public CreateTaskCommand toCommand(Long projectId) {
        return new CreateTaskCommand(
                projectId,
                parentId,
                taskCode,
                name,
                description,
                taskType != null ? taskType : TaskType.TASK,
                assigneeId,
                estimatedHours,
                sortOrder != null ? sortOrder : 0);
    }
}
