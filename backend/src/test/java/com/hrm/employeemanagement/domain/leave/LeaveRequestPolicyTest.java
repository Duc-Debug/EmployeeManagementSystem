package com.hrm.employeemanagement.domain.leave;

import com.hrm.employeemanagement.domain.exception.leave.InvalidLeaveDateRangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LeaveRequestPolicyTest {

    @Test
    @DisplayName("TC-03: Ngày kết thúc trước ngày bắt đầu phải ném InvalidLeaveDateRangeException")
    void shouldThrowWhenEndDateBeforeStartDate() {
        LocalDate start = LocalDate.of(2026, 4, 15);
        LocalDate end = LocalDate.of(2026, 4, 10);

        assertThrows(InvalidLeaveDateRangeException.class, () ->
                LeaveRequestPolicy.validateDateRange(start, end));
    }

    @Test
    @DisplayName("Tính đúng số ngày làm việc không tính T7 & CN")
    void shouldCalculateWorkingDaysExcludingWeekends() {
        // T2 (13/04/2026) đến T6 (17/04/2026): 5 ngày
        LocalDate monday = LocalDate.of(2026, 4, 13);
        LocalDate friday = LocalDate.of(2026, 4, 17);
        assertEquals(5, LeaveRequestPolicy.calculateWorkingDays(monday, friday));

        // T6 (17/04/2026) đến T2 tuần sau (20/04/2026): 2 ngày làm việc (T6 và T2)
        LocalDate nextMonday = LocalDate.of(2026, 4, 20);
        assertEquals(2, LeaveRequestPolicy.calculateWorkingDays(friday, nextMonday));

        // Nghỉ trúng T7 và CN: 0 ngày làm việc
        LocalDate sat = LocalDate.of(2026, 4, 18);
        LocalDate sun = LocalDate.of(2026, 4, 19);
        assertEquals(0, LeaveRequestPolicy.calculateWorkingDays(sat, sun));
    }

    @Test
    @DisplayName("Tính đúng số giờ nghỉ = số ngày làm việc * 8.0")
    void shouldCalculateDeductedHours() {
        LocalDate monday = LocalDate.of(2026, 4, 13);
        LocalDate wednesday = LocalDate.of(2026, 4, 15);
        int workingDays = LeaveRequestPolicy.calculateWorkingDays(monday, wednesday);
        BigDecimal hours = LeaveRequestPolicy.calculateHoursDeducted(workingDays, 40);
        assertEquals(new BigDecimal("24.00"), hours);
    }

    @Test
    @DisplayName("TC-02: Kiểm tra phát hiện khoảng ngày trùng nhau (Overlap)")
    void shouldDetectOverlappingRanges() {
        LocalDate s1 = LocalDate.of(2026, 4, 10);
        LocalDate e1 = LocalDate.of(2026, 4, 15);

        // Trường hợp trùng 1 phần
        assertTrue(LeaveRequestPolicy.isOverlapping(s1, e1, LocalDate.of(2026, 4, 12), LocalDate.of(2026, 4, 18)));
        // Trường hợp nằm trọn bên trong
        assertTrue(LeaveRequestPolicy.isOverlapping(s1, e1, LocalDate.of(2026, 4, 11), LocalDate.of(2026, 4, 14)));
        // Trường hợp bao trọn ra ngoài
        assertTrue(LeaveRequestPolicy.isOverlapping(s1, e1, LocalDate.of(2026, 4, 8), LocalDate.of(2026, 4, 20)));
        // Trường hợp trùng ngày biên
        assertTrue(LeaveRequestPolicy.isOverlapping(s1, e1, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 20)));

        // Không trùng (hoàn toàn trước hoặc sau)
        assertFalse(LeaveRequestPolicy.isOverlapping(s1, e1, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 9)));
        assertFalse(LeaveRequestPolicy.isOverlapping(s1, e1, LocalDate.of(2026, 4, 16), LocalDate.of(2026, 4, 25)));
    }
}
