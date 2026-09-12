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

    @Test
    @DisplayName("Cải tiến P1 & P2: Không cảnh báo ngày cuối tuần & ngày lễ, đồng thời tính đúng tổng giờ nghỉ trong ngày")
    void weekendAndHoliday_suppressesWarning_andCalculatesTotalHours() {
        Long orgUnitId = 10L;
        String orgUnitCode = "DEV";
        String orgUnitName = "Phòng Phát triển";
        int year = 2026;
        int month = 9;
        int totalEmployees = 5;

        // Ngày 2026-09-02 là ngày Lễ Quốc Khánh
        LocalDate holidaySep2 = LocalDate.of(2026, 9, 2);
        // Ngày 2026-09-06 là Chủ Nhật
        LocalDate sundaySep6 = LocalDate.of(2026, 9, 6);
        // Ngày 2026-09-07 là Thứ Hai (ngày làm việc)
        LocalDate mondaySep7 = LocalDate.of(2026, 9, 7);

        // 4 người nghỉ từ 01/09 đến 08/09 (có 5 ngày làm việc thực tế: 1, 3, 4, 7, 8; ngày 2 là lễ, 5-6 là T7/CN)
        // Số giờ tổng: EMP-01: 40h (8h/ngày), EMP-02: 20h (4h/ngày), EMP-03: 40h (8h/ngày), EMP-04: 40h (8h/ngày)
        List<LeaveCalendarItem> leaves = List.of(
                new LeaveCalendarItem(1L, 1L, "EMP-01", "A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8), LeaveStatus.APPROVED, new BigDecimal("40.00"), "ANNUAL", "Nghỉ"),
                new LeaveCalendarItem(2L, 2L, "EMP-02", "B", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8), LeaveStatus.APPROVED, new BigDecimal("20.00"), "ANNUAL", "Nghỉ"),
                new LeaveCalendarItem(3L, 3L, "EMP-03", "C", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8), LeaveStatus.PENDING, new BigDecimal("40.00"), "ANNUAL", "Nghỉ"),
                new LeaveCalendarItem(4L, 4L, "EMP-04", "D", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8), LeaveStatus.PENDING, new BigDecimal("40.00"), "ANNUAL", "Nghỉ")
        );

        com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar defaultCalendar =
                com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar.createDefault();
        java.util.Set<LocalDate> holidays = java.util.Set.of(holidaySep2);

        DepartmentMonthlyLeaveCalendar calendar = DepartmentMonthlyLeaveCalendar.calculate(
                orgUnitId,
                orgUnitCode,
                orgUnitName,
                year,
                month,
                totalEmployees,
                0.50,
                leaves,
                defaultCalendar,
                holidays
        );

        // 1. Kiểm tra ngày Lễ (02/09)
        DailyLeaveSummary sep2 = calendar.getDailySummaries().get(1); // 2/9
        assertEquals(holidaySep2, sep2.getDate());
        assertTrue(sep2.isHoliday(), "Ngày 2/9 phải là ngày lễ");
        assertFalse(sep2.isCompanyWorkingDay(), "Ngày lễ không phải là ngày làm việc của công ty");
        assertFalse(sep2.isWarning(), "Ngày lễ không được cảnh báo dù 4/5 người nghỉ");
        assertEquals(BigDecimal.ZERO, sep2.getTotalLeaveHours(), "Ngày lễ công ty không trừ giờ nghỉ làm việc (0h)");

        // 2. Kiểm tra ngày Chủ Nhật (06/09)
        DailyLeaveSummary sep6 = calendar.getDailySummaries().get(5); // 6/9
        assertEquals(sundaySep6, sep6.getDate());
        assertFalse(sep6.isCompanyWorkingDay(), "Chủ nhật không phải ngày làm việc");
        assertFalse(sep6.isWarning(), "Chủ nhật không được kích hoạt cảnh báo");
        assertEquals(BigDecimal.ZERO, sep6.getTotalLeaveHours(), "Ngày cuối tuần không trừ giờ làm việc (0h)");

        // 3. Kiểm tra ngày Thứ Hai (07/09)
        DailyLeaveSummary sep7 = calendar.getDailySummaries().get(6); // 7/9
        assertEquals(mondaySep7, sep7.getDate());
        assertTrue(sep7.isCompanyWorkingDay(), "Thứ Hai là ngày làm việc");
        assertTrue(sep7.isWarning(), "Thứ Hai có 4/5 người nghỉ -> Bắt buộc phải cảnh báo");
        assertEquals(new BigDecimal("28.00"), sep7.getTotalLeaveHours(), "Tổng giờ nghỉ tích lũy ngày 7/9: 8+4+8+8=28h");
    }

    @Test
    @DisplayName("Review Issue 2: Đơn nghỉ 3 ngày (24h) phân bổ đúng 8h/ngày làm việc, không cộng dồn thành 72h")
    void multiDayLeave_distributesDailyHoursAccurately_noDoubleCounting() {
        Long orgUnitId = 10L;
        String orgUnitCode = "DEV";
        String orgUnitName = "Phòng Phát triển";
        int year = 2026;
        int month = 9;
        int totalEmployees = 5;

        // Đơn nghỉ 3 ngày làm việc: 01/09 (Tue), 02/09 (Wed), 03/09 (Thu), tổng hoursDeducted = 24.00h
        List<LeaveCalendarItem> leaves = List.of(
                new LeaveCalendarItem(
                        1L, 101L, "DEV-01", "Nguyễn Văn A",
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3),
                        LeaveStatus.APPROVED, new BigDecimal("24.00"), "ANNUAL", "Nghỉ phép năm"
                )
        );

        DepartmentMonthlyLeaveCalendar calendar = DepartmentMonthlyLeaveCalendar.calculate(
                orgUnitId, orgUnitCode, orgUnitName, year, month, totalEmployees, 0.50, leaves
        );

        // Ngày 1/9 (Thứ Ba - làm việc): đúng 8.00h
        DailyLeaveSummary sep1 = calendar.getDailySummaries().get(0);
        assertEquals(new BigDecimal("8.00"), sep1.getTotalLeaveHours(), "Ngày 1/9 chỉ được tính 8h thay vì 24h");

        // Ngày 2/9 (Thứ Tư - làm việc): đúng 8.00h
        DailyLeaveSummary sep2 = calendar.getDailySummaries().get(1);
        assertEquals(new BigDecimal("8.00"), sep2.getTotalLeaveHours(), "Ngày 2/9 chỉ được tính 8h thay vì 24h");

        // Ngày 3/9 (Thứ Năm - làm việc): đúng 8.00h
        DailyLeaveSummary sep3 = calendar.getDailySummaries().get(2);
        assertEquals(new BigDecimal("8.00"), sep3.getTotalLeaveHours(), "Ngày 3/9 chỉ được tính 8h thay vì 24h");

        // Tổng cộng cả 3 ngày đúng 24h, không phải 72h!
        BigDecimal totalAcrossThreeDays = sep1.getTotalLeaveHours()
                .add(sep2.getTotalLeaveHours())
                .add(sep3.getTotalLeaveHours());
        assertEquals(new BigDecimal("24.00"), totalAcrossThreeDays, "Tổng giờ nghỉ cả 3 ngày phải đúng 24.00h");
    }
}
