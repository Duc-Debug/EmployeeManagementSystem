package com.hrm.employeemanagement.application.dto.task.cascade;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record EvaluateCascadeDelayCommand(
        @NotNull(message = "Mã dự án (projectId) không được null")
        Long projectId,

        @NotNull(message = "Mã công việc (taskId) không được null")
        Long taskId,

        @NotNull(message = "Ngày kết thúc thực tế mới không được null")
        LocalDate newActualEndDate
) {
}
