package com.hrm.employeemanagement.domain.availability;

import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("YearWeek Value Object Tests")
class YearWeekTest {

    @Test
    @DisplayName("Tạo YearWeek hợp lệ và tính ngày bắt đầu (Thứ 2) / kết thúc (Chủ Nhật)")
    void validYearWeek() {
        YearWeek yearWeek = YearWeek.of(2026, 36);

        assertEquals(2026, yearWeek.year());
        assertEquals(36, yearWeek.weekNumber());

        LocalDate startDate = yearWeek.getStartDate();
        LocalDate endDate = yearWeek.getEndDate();

        assertEquals(DayOfWeek.MONDAY, startDate.getDayOfWeek());
        assertEquals(DayOfWeek.SUNDAY, endDate.getDayOfWeek());
        assertEquals(6, startDate.until(endDate).getDays());
    }

    @Test
    @DisplayName("Ném InvalidWeekNumberException khi tuần hoặc năm không hợp lệ")
    void invalidWeekOrYear() {
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2026, 0));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2026, 54));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(1999, 10));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2101, 10));

        // Năm 2024 và 2025 chỉ có 52 tuần ISO -> Tuần 53 ném InvalidWeekNumberException
        assertEquals(52, YearWeek.maxWeeksInYear(2024));
        assertEquals(52, YearWeek.maxWeeksInYear(2025));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2024, 53));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2025, 53));

        // Năm 2020 và 2026 có 53 tuần ISO -> Tuần 53 hợp lệ, tuần 54 ném exception
        assertEquals(53, YearWeek.maxWeeksInYear(2020));
        assertEquals(53, YearWeek.maxWeeksInYear(2026));
        assertDoesNotThrow(() -> YearWeek.of(2020, 53));
        assertDoesNotThrow(() -> YearWeek.of(2026, 53));
    }
}
