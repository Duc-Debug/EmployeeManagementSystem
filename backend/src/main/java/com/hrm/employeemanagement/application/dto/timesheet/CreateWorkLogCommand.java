package com.hrm.employeemanagement.application.dto.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * Tạo dòng ghi mới
 */
public record CreateWorkLogCommand(
    Long projectId,       // ID dự án
    Long taskId,          // ID công việc
    LocalDate workDate,   // Ngày làm việc thực tế
    BigDecimal hours,     // Số giờ (ví dụ 4.0h)
    Boolean isBillable,   // Có tính phí khách hàng hay không
    String description    // Mô tả chi tiết nội dung đã làm
) {}