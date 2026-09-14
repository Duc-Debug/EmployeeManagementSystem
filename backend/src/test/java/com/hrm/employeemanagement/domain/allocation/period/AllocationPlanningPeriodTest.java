package com.hrm.employeemanagement.domain.allocation.period;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodStateException;

@DisplayName("AllocationPlanningPeriod Domain Tests (NCL-06-CN-009 / QTN-18)")
class AllocationPlanningPeriodTest {

    @Test
    @DisplayName("Khởi tạo thành công kỳ kế hoạch phân bổ với trạng thái mặc định là OPEN")
    void shouldCreateNewPeriodSuccessfully() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                "Kế hoạch Quý 1/2026",
                AllocationPeriodType.QUARTER,
                2026,
                1,
                13,
                1L
        );

        assertNotNull(period);
        assertNull(period.getId());
        assertEquals("Kế hoạch Quý 1/2026", period.getName());
        assertEquals(AllocationPeriodType.QUARTER, period.getPeriodType());
        assertEquals(2026, period.getYear());
        assertEquals(1, period.getStartWeek());
        assertEquals(13, period.getEndWeek());
        assertEquals(AllocationPeriodStatus.OPEN, period.getStatus());
        assertFalse(period.isLocked());
        assertNull(period.getLockedBy());
        assertNull(period.getLockedAt());
        assertNull(period.getUnlockedBy());
        assertNull(period.getUnlockedAt());
        assertNull(period.getUnlockReason());
        assertEquals(1L, period.getCreatedBy());
    }

    @Test
    @DisplayName("TC-01: Khóa kỳ kế hoạch thành công khi đang OPEN")
    void shouldLockPeriodSuccessfully() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                "Kế hoạch Quý 1/2026",
                AllocationPeriodType.QUARTER,
                2026,
                1,
                13,
                1L
        );

        period.lock(2L);

        assertEquals(AllocationPeriodStatus.LOCKED, period.getStatus());
        assertTrue(period.isLocked());
        assertEquals(2L, period.getLockedBy());
        assertNotNull(period.getLockedAt());
    }

    @Test
    @DisplayName("Ném InvalidAllocationPeriodStateException khi khóa kỳ đã ở trạng thái LOCKED")
    void shouldThrowWhenLockingAlreadyLockedPeriod() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                "Kế hoạch Quý 1/2026",
                AllocationPeriodType.QUARTER,
                2026,
                1,
                13,
                1L
        );
        period.lock(2L);

        assertThrows(InvalidAllocationPeriodStateException.class, () -> period.lock(3L));
    }

    @Test
    @DisplayName("TC-04: Mở lại kỳ kế hoạch thành công kèm lý do khi đang LOCKED")
    void shouldUnlockPeriodSuccessfully() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                "Kế hoạch Quý 1/2026",
                AllocationPeriodType.QUARTER,
                2026,
                1,
                13,
                1L
        );
        period.lock(2L);

        period.unlock(3L, "Điều chỉnh phân bổ theo yêu cầu của Ban giám đốc");

        assertEquals(AllocationPeriodStatus.OPEN, period.getStatus());
        assertFalse(period.isLocked());
        assertEquals(3L, period.getUnlockedBy());
        assertNotNull(period.getUnlockedAt());
        assertEquals("Điều chỉnh phân bổ theo yêu cầu của Ban giám đốc", period.getUnlockReason());
    }

    @Test
    @DisplayName("Ném InvalidAllocationPeriodStateException khi mở lại kỳ đang ở trạng thái OPEN")
    void shouldThrowWhenUnlockingOpenPeriod() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                "Kế hoạch Quý 1/2026",
                AllocationPeriodType.QUARTER,
                2026,
                1,
                13,
                1L
        );

        assertThrows(InvalidAllocationPeriodStateException.class, () ->
                period.unlock(2L, "Lý do mở lại")
        );
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi mở lại kỳ mà lý do bị bỏ trống")
    void shouldThrowWhenUnlockingWithoutReason() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                "Kế hoạch Quý 1/2026",
                AllocationPeriodType.QUARTER,
                2026,
                1,
                13,
                1L
        );
        period.lock(2L);

        assertThrows(IllegalArgumentException.class, () -> period.unlock(3L, "   "));
        assertThrows(IllegalArgumentException.class, () -> period.unlock(3L, null));
    }

    @Test
    @DisplayName("Kiểm tra tuần nằm trong hoặc ngoài kỳ kế hoạch chính xác")
    void shouldCheckWeekWithinPeriodCorrectly() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                "Kế hoạch Quý 2/2026",
                AllocationPeriodType.QUARTER,
                2026,
                14,
                26,
                1L
        );

        assertTrue(period.isWeekWithin(2026, 14));
        assertTrue(period.isWeekWithin(2026, 20));
        assertTrue(period.isWeekWithin(2026, 26));

        assertFalse(period.isWeekWithin(2026, 13));
        assertFalse(period.isWeekWithin(2026, 27));
        assertFalse(period.isWeekWithin(2025, 20));
    }

    @Test
    @DisplayName("Ném InvalidAllocationPeriodException khi tuần bắt đầu > tuần kết thúc")
    void shouldThrowWhenStartWeekGreaterThanEndWeek() {
        assertThrows(InvalidAllocationPeriodException.class, () ->
                AllocationPlanningPeriod.createNew("Kỳ lỗi", AllocationPeriodType.CUSTOM, 2026, 20, 10, 1L)
        );
    }

    @Test
    @DisplayName("Ném InvalidAllocationPeriodException khi tuần ngoài khoảng 1..53")
    void shouldThrowWhenWeekOutOfRange() {
        assertThrows(InvalidAllocationPeriodException.class, () ->
                AllocationPlanningPeriod.createNew("Kỳ lỗi", AllocationPeriodType.CUSTOM, 2026, 0, 10, 1L)
        );
        assertThrows(InvalidAllocationPeriodException.class, () ->
                AllocationPlanningPeriod.createNew("Kỳ lỗi", AllocationPeriodType.CUSTOM, 2026, 1, 54, 1L)
        );
    }
}
