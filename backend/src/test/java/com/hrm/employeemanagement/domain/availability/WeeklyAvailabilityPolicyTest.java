package com.hrm.employeemanagement.domain.availability;

import com.hrm.employeemanagement.domain.exception.availability.InvalidAvailabilityHoursException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QTN-10: Năng lực khả dụng trừ ngày lễ và nghỉ phép Unit Tests")
class WeeklyAvailabilityPolicyTest {

    @Test
    @DisplayName("TC-QTN10-01: Tuần làm việc tiêu chuẩn không có lễ và nghỉ phép -> Khả dụng = Giờ chuẩn")
    void tc_qtn10_01_standardWeek() {
        int standardHours = 40;
        int holidayHours = 0;
        BigDecimal approvedLeaveHours = BigDecimal.ZERO;

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);

        assertEquals(new BigDecimal("40.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-02: Tuần có 1 ngày lễ trong tuần làm việc (8h) -> Khả dụng = 32h")
    void tc_qtn10_02_oneHolidayInWeek() {
        YearWeek yearWeek = YearWeek.of(2026, 36); // Tuần 36 năm 2026
        LocalDate wednesday = yearWeek.getStartDate().plusDays(2); // Thứ Tư

        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHours(yearWeek, List.of(wednesday));
        assertEquals(8, holidayHours);

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(40, holidayHours, BigDecimal.ZERO);
        assertEquals(new BigDecimal("32.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-03: Tuần có 3 ngày lễ trong tuần làm việc (24h) -> Khả dụng = 16h")
    void tc_qtn10_03_multipleHolidays() {
        YearWeek yearWeek = YearWeek.of(2026, 5); // Dịp Tết
        LocalDate monday = yearWeek.getStartDate();
        LocalDate tuesday = yearWeek.getStartDate().plusDays(1);
        LocalDate wednesday = yearWeek.getStartDate().plusDays(2);

        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHours(yearWeek, List.of(monday, tuesday, wednesday));
        assertEquals(24, holidayHours);

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(40, holidayHours, BigDecimal.ZERO);
        assertEquals(new BigDecimal("16.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-04: Ngày lễ rơi vào Thứ Bảy hoặc Chủ Nhật -> Không trừ giờ lễ khỏi giờ chuẩn")
    void tc_qtn10_04_holidayOnWeekend() {
        YearWeek yearWeek = YearWeek.of(2026, 36);
        LocalDate sunday = yearWeek.getEndDate(); // Chủ Nhật

        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHours(yearWeek, List.of(sunday));
        assertEquals(0, holidayHours, "Lễ rơi vào cuối tuần không được trừ vào giờ chuẩn hành chính");

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(40, holidayHours, BigDecimal.ZERO);
        assertEquals(new BigDecimal("40.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-05: Tuần có đơn nghỉ phép ĐÃ DUYỆT (16h) -> Khả dụng = 24h")
    void tc_qtn10_05_approvedLeave() {
        int standardHours = 40;
        int holidayHours = 0;
        BigDecimal approvedLeaveHours = new BigDecimal("16.00");

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);

        assertEquals(new BigDecimal("24.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-06 & TC-QTN10-07: Đơn nghỉ phép chưa duyệt hoặc bị từ chối không được tính vào approvedLeaveHours")
    void tc_qtn10_06_pendingLeaveIgnored() {
        int standardHours = 40;
        int holidayHours = 0;
        // Theo QTN-10: Chỉ truyền số giờ APPROVED vào tính toán. Đơn PENDING hoặc REJECTED được coi là 0h
        BigDecimal approvedLeaveHours = BigDecimal.ZERO;

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);

        assertEquals(new BigDecimal("40.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-08: Kết hợp: 1 ngày lễ (8h) + 1 ngày phép đã duyệt (8h) -> Khả dụng = 24h")
    void tc_qtn10_08_combinedHolidayAndLeave() {
        int standardHours = 40;
        int holidayHours = 8;
        BigDecimal approvedLeaveHours = new BigDecimal("8.00");

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);

        assertEquals(new BigDecimal("24.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-09: Nghỉ phép nửa ngày (4h) đã duyệt -> Khả dụng = 36h")
    void tc_qtn10_09_halfDayLeave() {
        int standardHours = 40;
        int holidayHours = 0;
        BigDecimal approvedLeaveHours = new BigDecimal("4.00");

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);

        assertEquals(new BigDecimal("36.00"), netHours);
    }

    @Test
    @DisplayName("TC-QTN10-10: Tổng giờ lễ và phép vượt quá giờ chuẩn -> Khả dụng không âm (về 0h)")
    void tc_qtn10_10_cappedAtZero() {
        int standardHours = 40;
        int holidayHours = 24;
        BigDecimal approvedLeaveHours = new BigDecimal("24.00"); // Tổng trừ = 48h > 40h

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);

        assertEquals(new BigDecimal("0.00"), netHours);
    }

    @Test
    @DisplayName("Validation: Số giờ chuẩn <= 0 hoặc > 168 giờ phải ném ngoại lệ InvalidAvailabilityHoursException")
    void validateStandardHours_Invalid() {
        assertThrows(InvalidAvailabilityHoursException.class, () ->
                WeeklyAvailabilityPolicy.validateStandardHours(0));
        assertThrows(InvalidAvailabilityHoursException.class, () ->
                WeeklyAvailabilityPolicy.validateStandardHours(-5));
        assertThrows(InvalidAvailabilityHoursException.class, () ->
                WeeklyAvailabilityPolicy.validateStandardHours(169));
    }

    @Test
    @DisplayName("TC-QTN10-11: Ngày lễ với số giờ khấu trừ linh hoạt (4h nửa ngày) -> Khả dụng tính chính xác")
    void tc_qtn10_11_dynamicHolidayHours() {
        YearWeek yearWeek = YearWeek.of(2026, 36);
        LocalDate friday = yearWeek.getStartDate().plusDays(4);

        Holiday halfDayHoliday = new Holiday(friday, "Nghỉ lễ nửa ngày", 4);
        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yearWeek, List.of(halfDayHoliday));

        assertEquals(4, holidayHours);

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(40, holidayHours, BigDecimal.ZERO);
        assertEquals(new BigDecimal("36.00"), netHours);
    }

    @Test
    @DisplayName("QTN-10: Đơn nghỉ phép kéo dài nhiều tuần (9 ngày = 72h) -> Phân bổ đúng 32h cho Tuần 36 và 40h cho Tuần 37")
    void tc_qtn10_multiWeekLeavePartition() {
        YearWeek week36 = YearWeek.of(2026, 36); // 2026-08-31 (Mon) to 2026-09-06 (Sun)
        YearWeek week37 = YearWeek.of(2026, 37); // 2026-09-07 (Mon) to 2026-09-13 (Sun)

        // Đơn nghỉ phép: 2026-09-01 (Tue) đến 2026-09-11 (Fri) = 9 working days, 72 hours
        LocalDate leaveStart = LocalDate.of(2026, 9, 1);
        LocalDate leaveEnd = LocalDate.of(2026, 9, 11);
        BigDecimal totalHours = new BigDecimal("72.00");

        BigDecimal week36Hours = WeeklyAvailabilityPolicy.calculateLeaveHoursInWindow(
                leaveStart, leaveEnd, totalHours, week36.getStartDate(), week36.getEndDate());
        BigDecimal week37Hours = WeeklyAvailabilityPolicy.calculateLeaveHoursInWindow(
                leaveStart, leaveEnd, totalHours, week37.getStartDate(), week37.getEndDate());

        // Tuần 36 có 4 ngày làm việc (Tue, Wed, Thu, Fri) -> 72 * 4 / 9 = 32.00h
        assertEquals(new BigDecimal("32.00"), week36Hours);
        // Tuần 37 có 5 ngày làm việc (Mon, Tue, Wed, Thu, Fri) -> 72 * 5 / 9 = 40.00h
        assertEquals(new BigDecimal("40.00"), week37Hours);
        // Tổng phân bổ đúng bằng 72.00h
        assertEquals(totalHours, week36Hours.add(week37Hours));
    }

    @Test
    @DisplayName("QTN-10: Đơn nghỉ phép vắt qua cuối tuần (Thứ Sáu đến Thứ Hai: 16h) -> Phân bổ 8h mỗi tuần")
    void tc_qtn10_weekendSpanningLeavePartition() {
        YearWeek week36 = YearWeek.of(2026, 36);
        YearWeek week37 = YearWeek.of(2026, 37);

        // Thứ Sáu tuần 36 (2026-09-04) đến Thứ Hai tuần 37 (2026-09-07) = 2 working days (16h)
        LocalDate friday = LocalDate.of(2026, 9, 4);
        LocalDate monday = LocalDate.of(2026, 9, 7);
        BigDecimal totalHours = new BigDecimal("16.00");

        BigDecimal week36Hours = WeeklyAvailabilityPolicy.calculateLeaveHoursInWindow(
                friday, monday, totalHours, week36.getStartDate(), week36.getEndDate());
        BigDecimal week37Hours = WeeklyAvailabilityPolicy.calculateLeaveHoursInWindow(
                friday, monday, totalHours, week37.getStartDate(), week37.getEndDate());

        assertEquals(new BigDecimal("8.00"), week36Hours);
        assertEquals(new BigDecimal("8.00"), week37Hours);
        assertEquals(totalHours, week36Hours.add(week37Hours));
    }

    @Test
    @DisplayName("QTN-10: Đơn nghỉ nửa ngày (4h) nằm trọn trong tuần -> Giữ nguyên 4.00h")
    void tc_qtn10_halfDayLeaveInWeek() {
        YearWeek week36 = YearWeek.of(2026, 36);
        LocalDate wednesday = LocalDate.of(2026, 9, 2);

        BigDecimal hours = WeeklyAvailabilityPolicy.calculateLeaveHoursInWindow(
                wednesday, wednesday, new BigDecimal("4.00"), week36.getStartDate(), week36.getEndDate());

        assertEquals(new BigDecimal("4.00"), hours);
    }

    @Test
    @DisplayName("QTN-10: Đơn nghỉ nằm ngoài tuần hoàn toàn -> 0.00h")
    void tc_qtn10_leaveOutsideWeek() {
        YearWeek week36 = YearWeek.of(2026, 36);
        LocalDate oct1 = LocalDate.of(2026, 10, 1);
        LocalDate oct5 = LocalDate.of(2026, 10, 5);

        BigDecimal hours = WeeklyAvailabilityPolicy.calculateLeaveHoursInWindow(
                oct1, oct5, new BigDecimal("24.00"), week36.getStartDate(), week36.getEndDate());

        assertEquals(new BigDecimal("0.00"), hours);
    }
}
