package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * NCL-06-CN-006 & NCL-06-CN-007: Command chứa dữ liệu yêu cầu phân bổ nguồn lực hàng loạt cho nhiều tuần.
 * Hỗ trợ phân bổ theo số giờ cố định (allocatedHoursPerWeek) hoặc theo tỷ lệ phần trăm (allocationPercentagePerWeek).
 */
public record BulkAllocateResourceCommand(
        Long employeeId,
        Long projectId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        BigDecimal allocatedHoursPerWeek,
        BigDecimal allocationPercentagePerWeek
) {
    public BulkAllocateResourceCommand(
            Long employeeId,
            Long projectId,
            Integer fromYear,
            Integer fromWeek,
            Integer toYear,
            Integer toWeek,
            BigDecimal allocatedHoursPerWeek
    ) {
        this(employeeId, projectId, fromYear, fromWeek, toYear, toWeek, allocatedHoursPerWeek, null);
    }

    public BulkAllocateResourceCommand {
        Objects.requireNonNull(employeeId, "ID nhân sự không được null");
        Objects.requireNonNull(projectId, "ID dự án không được null");
        Objects.requireNonNull(fromYear, "Năm bắt đầu không được null");
        Objects.requireNonNull(fromWeek, "Tuần bắt đầu không được null");
        Objects.requireNonNull(toYear, "Năm kết thúc không được null");
        Objects.requireNonNull(toWeek, "Tuần kết thúc không được null");

        if (allocatedHoursPerWeek == null && allocationPercentagePerWeek == null) {
            throw new IllegalArgumentException("Phải cung cấp số giờ phân bổ hoặc tỷ lệ phần trăm phân bổ mỗi tuần");
        }

        if (allocatedHoursPerWeek != null && allocationPercentagePerWeek != null) {
            throw new IllegalArgumentException("Không được cung cấp đồng thời số giờ phân bổ và tỷ lệ phần trăm phân bổ mỗi tuần");
        }

        if (allocatedHoursPerWeek != null) {
            if (allocatedHoursPerWeek.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số giờ phân bổ mỗi tuần phải lớn hơn 0");
            }
            if (allocatedHoursPerWeek.compareTo(BigDecimal.valueOf(168)) > 0) {
                throw new IllegalArgumentException("Số giờ phân bổ mỗi tuần không được vượt quá 168 giờ");
            }
        }

        if (allocationPercentagePerWeek != null) {
            if (allocationPercentagePerWeek.compareTo(BigDecimal.ZERO) < 0 || allocationPercentagePerWeek.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Tỷ lệ phần trăm phân bổ mỗi tuần phải nằm trong khoảng từ 0% đến 100%");
            }
        }
    }
}
