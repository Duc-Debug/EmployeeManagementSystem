package com.hrm.employeemanagement.domain.unavailability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnavailabilityPolicyTest {

    @Test
    @DisplayName("Đếm chính xác số ngày làm việc trong tuần (bỏ qua Thứ 7 và Chủ nhật)")
    void testCountWorkingDaysExcludesWeekends() {
        // Thứ 6 đến Thứ 2 tuần sau (4 ngày dương lịch: T6, T7, CN, T2) -> 2 ngày làm việc (T6, T2)
        LocalDate start = LocalDate.of(2026, 9, 18); // Friday
        LocalDate end = LocalDate.of(2026, 9, 21);   // Monday

        int count = UnavailabilityPolicy.countWorkingDays(start, end, null);
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Tính tổng số giờ không sẵn sàng: 2 ngày làm việc = 16 giờ")
    void testCalculateTotalHoursForTwoDays() {
        LocalDate start = LocalDate.of(2026, 9, 22); // Tuesday
        LocalDate end = LocalDate.of(2026, 9, 23);   // Wednesday

        BigDecimal hours = UnavailabilityPolicy.calculateTotalHours(start, end, null);
        assertEquals(BigDecimal.valueOf(16.00).setScale(2), hours);
    }

    @Test
    @DisplayName("Tính số giờ không sẵn sàng trong một tuần cụ thể khi đơn trải dài")
    void testCalculateHoursInWindow() {
        LocalDate start = LocalDate.of(2026, 9, 21); // Monday week 1
        LocalDate end = LocalDate.of(2026, 9, 23);   // Wednesday week 1
        BigDecimal totalHours = BigDecimal.valueOf(24.00);

        LocalDate windowStart = LocalDate.of(2026, 9, 21);
        LocalDate windowEnd = LocalDate.of(2026, 9, 27);

        BigDecimal hoursInWindow = UnavailabilityPolicy.calculateHoursInWindow(
                start, end, totalHours, windowStart, windowEnd, null);

        assertEquals(BigDecimal.valueOf(24.00).setScale(2), hoursInWindow);
    }
}
