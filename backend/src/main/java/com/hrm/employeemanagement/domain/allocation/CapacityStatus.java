package com.hrm.employeemanagement.domain.allocation;

/**
 * Trạng thái năng lực phân bổ nhân sự theo tuần:
 * - OVERLOADED: Quá tải (allocatedHours > availableHours hoặc utilization > 100%) theo QTN-12
 * - OPTIMAL: Tối ưu (50% <= utilization <= 100%)
 * - UNDERUTILIZED: Nhàn rỗi (utilization < 50%)
 */
public enum CapacityStatus {
    OVERLOADED,
    OPTIMAL,
    UNDERUTILIZED
}
