package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;

/**
 * Chi tiết công suất và số giờ còn rảnh của 1 tuần cụ thể.
 */
public record WeeklyAvailableHoursResult(
        int year,
        int weekNumber,
        int standardHours,
        BigDecimal netAvailableHours, // Giờ làm việc khả dụng sau khi trừ nghỉ lễ & phép
        BigDecimal totalAllocatedHours, // Tổng giờ đã bị book vào các dự án
        BigDecimal remainingHours // Số giờ CÒN RẢNH (netAvailableHours - totalAllocatedHours)
        ) {

}
