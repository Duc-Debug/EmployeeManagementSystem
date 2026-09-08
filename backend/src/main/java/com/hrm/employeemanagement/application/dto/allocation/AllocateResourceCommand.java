package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.util.Objects;

public record AllocateResourceCommand(
        Long employeeId,
        Long projectId,
        Integer year,
        Integer weekNumber,
        BigDecimal allocatedHours
        ) {

    public AllocateResourceCommand {
        Objects.requireNonNull(employeeId, "ID nhân sự không được null");
        Objects.requireNonNull(projectId, "ID dự án không được null");
        Objects.requireNonNull(year, "Năm không được null");
        Objects.requireNonNull(weekNumber, "Số tuần không được null");
        Objects.requireNonNull(allocatedHours, "Số giờ phân bổ không được null");
    }
}
