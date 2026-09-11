package com.hrm.employeemanagement.domain.leave;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DepartmentMonthlyLeaveCalendar Domain Tests (NCL-05-CN-006)")
class DepartmentMonthlyLeaveCalendarTest {

    @Test
    @DisplayName("TC-01: Bộ phận có 6 đơn nghỉ trong tháng -> Lịch hiện đủ 6 khoảng nghỉ trên lịch tháng")
    void tc01_sixLeaveRequestsInMonth_allDisplayed() {
        Long orgUnitId = 10L;
        String orgUnitCode = "DEV";
        String orgUnitName = "Phòng Phát triển phần mềm";
        int year = 2026;
        int month = 9;
        int totalEmployees = 10;

        // Dữ liệu kiểm thử: Sáu đơn nghỉ mô phỏng
        List<LeaveCalendarItem> sixLeaves = List.of(
                new LeaveCalendarItem(1L, 101L, "DEV-01", "Nguyễn Văn A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), LeaveStatus.APPROVED, new BigDecimal("24.00"), "ANNUAL", "Nghỉ phép năm"),
                new LeaveCalendarItem(2L, 102L, "DEV-02", "Trần Thị B", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8), LeaveStatus.PENDING, new BigDecimal("16.00"), "ANNUAL", "Việc gia đình"),
                new LeaveCalendarItem(3L, 103L, "DEV-03", "Lê Văn C", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), LeaveStatus.APPROVED, new BigDecimal("24.00"), "ANNUAL", "Nghỉ mát"),
                new LeaveCalendarItem(4L, 104L, "DEV-04", "Phạm Thị D", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15), LeaveStatus.APPROVED, new BigDecimal("8.00"), "SICK", "Khám sức khỏe"),
                new LeaveCalendarItem(5L, 105L, "DEV-05", "Hoàng Văn E", LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 22), LeaveStatus.PENDING, new BigDecimal("24.00"), "ANNUAL", "Nghỉ cưới"),
                new LeaveCalendarItem(6L, 106L, "DEV-06", "Đỗ Thị F", LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 30), LeaveStatus.APPROVED, new BigDecimal("24.00"), "ANNUAL", "Nghỉ phép năm")
        );

        DepartmentMonthlyLeaveCalendar calendar = DepartmentMonthlyLeaveCalendar.calculate(
                orgUnitId,
                orgUnitCode,
                orgUnitName,
                year,
                month,
                totalEmployees,
                0.50,
                sixLeaves
        );

        assertEquals(6, calendar.getTotalLeaveRequests(), "Hệ thống phải hiện đủ 6 khoảng nghỉ");
        assertEquals(30, calendar.getDailySummaries().size(), "Tháng 9/2026 có 30 ngày");

        // Kiểm tra ngày 2026-09-02 (nằm trong đơn của DEV-01)
        DailyLeaveSummary sep2 = calendar.getDailySummaries().get(1); // day index 1 = Sep 2
        assertEquals(LocalDate.of(2026, 9, 2), sep2.getDate());
        assertEquals(1, sep2.getTotalOnLeave());
        assertEquals(1, sep2.getApprovedCount());
        assertEquals(0, sep2.getPendingCount());
        assertFalse(sep2.isWarning());

        // Kiểm tra ngày 2026-09-07 (nằm trong đơn PENDING của DEV-02)
        DailyLeaveSummary sep7 = calendar.getDailySummaries().get(6); // day index 6 = Sep 7
        assertEquals(LocalDate.of(2026, 9, 7), sep7.getDate());
        assertEquals(1, sep7.getTotalOnLeave());
        assertEquals(0, sep7.getApprovedCount());
        assertEquals(1, sep7.getPendingCount());
        assertFalse(sep7.isWarning());
    }

    @Test
    @DisplayName("TC-02: Bốn trên năm người trong bộ phận cùng nghỉ một ngày -> Hệ thống tô cảnh báo ngày đó")
    void tc02_fourOutOfFiveEmployeesOnSameDay_dayIsMarkedWarning() {
        Long orgUnitId = 10L;
        String orgUnitCode = "DEV";
        String orgUnitName = "Phòng Phát triển";
        int year = 2026;
        int month = 9;
        int totalEmployees = 5; // Tổng 5 người

        LocalDate criticalDay = LocalDate.of(2026, 9, 15);

        // 4 người cùng nghỉ ngày 15/9
        List<LeaveCalendarItem> leaves = List.of(
                new LeaveCalendarItem(1L, 1L, "EMP-01", "Nhân viên 1", criticalDay, criticalDay, LeaveStatus.APPROVED, new BigDecimal("8.00"), "ANNUAL", "Nghỉ"),
                new LeaveCalendarItem(2L, 2L, "EMP-02", "Nhân viên 2", criticalDay, criticalDay, LeaveStatus.APPROVED, new BigDecimal("8.00"), "ANNUAL", "Nghỉ"),
                new LeaveCalendarItem(3L, 3L, "EMP-03", "Nhân viên 3", criticalDay, criticalDay, LeaveStatus.PENDING, new BigDecimal("8.00"), "ANNUAL", "Nghỉ"),
                new LeaveCalendarItem(4L, 4L, "EMP-04", "Nhân viên 4", criticalDay, criticalDay, LeaveStatus.PENDING, new BigDecimal("8.00"), "ANNUAL", "Nghỉ")
        );

        DepartmentMonthlyLeaveCalendar calendar = DepartmentMonthlyLeaveCalendar.calculate(
                orgUnitId,
                orgUnitCode,
                orgUnitName,
                year,
                month,
                totalEmployees,
                0.50, // Ngưỡng 50%
                leaves
        );

        DailyLeaveSummary day15 = calendar.getDailySummaries().get(14); // Sep 15
        assertEquals(criticalDay, day15.getDate());
        assertEquals(4, day15.getTotalOnLeave(), "4 người nghỉ trong ngày");
        assertEquals(2, day15.getApprovedCount());
        assertEquals(2, day15.getPendingCount());
        assertTrue(day15.isWarning(), "Hệ thống phải tô cảnh báo ngày đó vì số người nghỉ vượt ngưỡng");
        assertNotNull(day15.getWarningMessage());
        assertTrue(day15.getWarningMessage().contains("4/5"));
        assertEquals(1, calendar.getWarningDaysCount(), "Tổng số ngày cảnh báo trong tháng là 1");
    }
}
