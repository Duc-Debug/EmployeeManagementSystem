package com.hrm.employeemanagement.application.dto.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
/**
 * Mô hình dữ liệu toàn diện cho trang giao diện Bảng chấm công tuần (/work-logs).
 */
public record WeeklyTimesheetResult(
    Long timesheetId,
    Long employeeId,
    String employeeName,
    LocalDate weekStartDate,         // Ngày đầu tuần (Thứ Hai)
    LocalDate weekEndDate,           // Ngày cuối tuần (Chủ Nhật)
    BigDecimal totalHours,           // Tổng giờ cả tuần (Target 40h)
    String status,                   // DRAFT, SUBMITTED, APPROVED...
    boolean isEditable,              // true nếu bảng còn ở DRAFT (được sửa/xóa)
    List<DailyWorkLogGroupDto> dailyGroups, // 7 nhóm ngày từ Thứ 2 -> Chủ Nhật
    List<WorkLogResult> allEntries   // Toàn bộ các dòng giờ trong tuần
) {}