package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * NCL-06-CN-006: Command chứa dữ liệu yêu cầu phân bổ nguồn lực hàng loạt cho nhiều tuần.
 */
public record BulkAllocateResourceCommand(
        Long employeeId,
        Long projectId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        BigDecimal allocatedHoursPerWeek
) {
    public BulkAllocateResourceCommand {
        Objects.requireNonNull(employeeId, "ID nhân sự không được null");
        Objects.requireNonNull(projectId, "ID dự án không được null");
        Objects.requireNonNull(fromYear, "Năm bắt đầu không được null");
        Objects.requireNonNull(fromWeek, "Tuần bắt đầu không được null");
        Objects.requireNonNull(toYear, "Năm kết thúc không được null");
        Objects.requireNonNull(toWeek, "Tuần kết thúc không được null");
        Objects.requireNonNull(allocatedHoursPerWeek, "Số giờ phân bổ mỗi tuần không được null");
        if (allocatedHoursPerWeek.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số giờ phân bổ mỗi tuần phải lớn hơn 0");
        }
        if (allocatedHoursPerWeek.compareTo(BigDecimal.valueOf(168)) > 0) {
            throw new IllegalArgumentException("Số giờ phân bổ mỗi tuần không được vượt quá 168 giờ");
        }
    }
}
