package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.util.Objects;

public record AllocateResourceCommand(
        Long employeeId,
        Long projectId,
        Integer year,
        Integer weekNumber,
        BigDecimal allocatedHours,
        BigDecimal allocationPercentage,
        String overloadReason
) {

    public AllocateResourceCommand(Long employeeId, Long projectId, Integer year, Integer weekNumber, BigDecimal allocatedHours) {
        this(employeeId, projectId, year, weekNumber, allocatedHours, null, null);
    }

    public AllocateResourceCommand(Long employeeId, Long projectId, Integer year, Integer weekNumber, BigDecimal allocatedHours, BigDecimal allocationPercentage) {
        this(employeeId, projectId, year, weekNumber, allocatedHours, allocationPercentage, null);
    }

    public AllocateResourceCommand {
        Objects.requireNonNull(employeeId, "ID nhân sự không được null");
        Objects.requireNonNull(projectId, "ID dự án không được null");
        Objects.requireNonNull(year, "Năm không được null");
        Objects.requireNonNull(weekNumber, "Số tuần không được null");
        if (allocatedHours == null && allocationPercentage == null) {
            throw new IllegalArgumentException("Phải cung cấp số giờ phân bổ hoặc tỷ lệ phần trăm phân bổ");
        }
    }
}
