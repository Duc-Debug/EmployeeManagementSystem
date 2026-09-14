package com.hrm.employeemanagement.domain.allocation.period;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException;

@DisplayName("AllocationPeriodLockPolicy Domain Policy Tests (QTN-18 / TC-02)")
class AllocationPeriodLockPolicyTest {

    @Test
    @DisplayName("Tìm thấy kỳ bị khóa bao phủ tuần và năm tương ứng")
    void shouldFindLockedPeriodCoveringWeek() {
        AllocationPlanningPeriod period1 = AllocationPlanningPeriod.createNew("Q1", AllocationPeriodType.QUARTER, 2026, 1, 13, 1L);
        period1.lock(2L);

        AllocationPlanningPeriod period2 = AllocationPlanningPeriod.createNew("Q2", AllocationPeriodType.QUARTER, 2026, 14, 26, 1L);
        // period2 is OPEN

        List<AllocationPlanningPeriod> periods = List.of(period1, period2);

        Optional<AllocationPlanningPeriod> lockedOpt = AllocationPeriodLockPolicy.findLockedPeriodCoveringWeek(2026, 5, periods);
        assertTrue(lockedOpt.isPresent());
        assertEquals("Q1", lockedOpt.get().getName());

        assertTrue(AllocationPeriodLockPolicy.isWeekLocked(2026, 5, periods));

        // Tuần 15 thuộc period2 nhưng period2 đang OPEN
        assertFalse(AllocationPeriodLockPolicy.isWeekLocked(2026, 15, periods));

        // Tuần ngoài khoảng
        assertFalse(AllocationPeriodLockPolicy.isWeekLocked(2026, 30, periods));
    }

    @Test
    @DisplayName("TC-02 (Sai trạng thái - QTN-18): Ném AllocationPeriodLockedException khi cố sửa phân bổ trong kỳ đã khóa")
    void shouldThrowAllocationPeriodLockedExceptionWhenPeriodLocked() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew("Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13, 1L);
        period.lock(2L);

        List<AllocationPlanningPeriod> periods = List.of(period);

        AllocationPeriodLockedException ex = assertThrows(
                AllocationPeriodLockedException.class,
                () -> AllocationPeriodLockPolicy.validateCanModifyAllocation(2026, 10, periods)
        );

        assertTrue(ex.getMessage().contains("Kế hoạch Quý 1/2026"));
        assertTrue(ex.getMessage().contains("QTN-18"));
        assertEquals("Kế hoạch Quý 1/2026", ex.getPeriodName());
        assertEquals(2026, ex.getYear());
        assertEquals(10, ex.getWeekNumber());
    }

    @Test
    @DisplayName("Cho phép sửa phân bổ khi kỳ đang mở (OPEN) theo QTN-18")
    void shouldPassWhenPeriodIsOpen() {
        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew("Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13, 1L);
        List<AllocationPlanningPeriod> periods = List.of(period);

        assertDoesNotThrow(() -> AllocationPeriodLockPolicy.validateCanModifyAllocation(2026, 10, periods));
    }
}
