package com.hrm.employeemanagement.application.dto.task;

import java.time.LocalDate;
import java.util.List;

public record AssignTaskCommand(
        Long projectId,
        Long taskId,
        List<Long> employeeIds,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate
) {
    public AssignTaskCommand {
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("Mã dự án (projectId) phải lớn hơn 0");
        }
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("Mã công việc (taskId) phải lớn hơn 0");
        }
        if (employeeIds == null) {
            employeeIds = List.of();
        }
        if (plannedStartDate != null && plannedEndDate != null && plannedEndDate.isBefore(plannedStartDate)) {
            throw new IllegalArgumentException("Ngày kết thúc mong muốn không được trước ngày bắt đầu mong muốn");
        }
    }
}

