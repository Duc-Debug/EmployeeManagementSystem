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
}
