package com.hrm.employeemanagement.application.dto.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
/**
 * Gom toàn bộ các dòng ghi giờ của một ngày cụ thể lại với nhau
 */
public record DailyWorkLogGroupDto(
    LocalDate date,                 // Ngày (vd: 2026-09-14)
    String dayOfWeek,               // Thứ trong tuần (vd: "Thứ Hai (14/09)")
    BigDecimal totalHours,          // Tổng giờ làm trong ngày đó
    boolean isExceededLimit,        // Cờ cảnh báo: true nếu > 12h (QTN-09)
    List<WorkLogResult> entries     // Danh sách các dòng giờ công trong ngày
) {}