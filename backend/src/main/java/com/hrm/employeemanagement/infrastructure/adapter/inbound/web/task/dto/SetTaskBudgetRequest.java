package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import java.math.BigDecimal;

import com.hrm.employeemanagement.application.dto.task.SetTaskBudgetCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SetTaskBudgetRequest(
        @NotNull(message = "Ngân sách giờ công không được để trống")
        @DecimalMin(value = "0.01", inclusive = true, message = "Ngân sách giờ công phải lớn hơn 0")
        @Digits(integer = 8, fraction = 2, message = "Ngân sách giờ công chỉ được có tối đa 8 chữ số phần nguyên và 2 chữ số phần thập phân")
        BigDecimal budgetHours
) {
    public SetTaskBudgetCommand toCommand(Long projectId, Long taskId) {
        return new SetTaskBudgetCommand(projectId, taskId, budgetHours);
    }
}
