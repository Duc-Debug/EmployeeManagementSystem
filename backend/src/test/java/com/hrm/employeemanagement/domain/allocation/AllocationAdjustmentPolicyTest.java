package com.hrm.employeemanagement.domain.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.allocation.CannotRemoveAllocationWithActualHoursException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationAdjustmentException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AllocationAdjustmentPolicy Domain Unit Tests")
class AllocationAdjustmentPolicyTest {

    private final YearWeek pastWeek = new YearWeek(2026, 1);
    private final YearWeek currentWeek = new YearWeek(2026, 10);
    private final YearWeek futureWeek = new YearWeek(2026, 40);
    private final LocalDate today = LocalDate.of(2026, 3, 9); // Week 11 Monday

    @Nested
    @DisplayName("isWeekEnded Tests")
    class IsWeekEndedTests {

        @Test
        @DisplayName("Week ending before today should be recognized as ended")
        void pastWeekIsEnded() {
            assertTrue(AllocationAdjustmentPolicy.isWeekEnded(pastWeek, today));
        }

        @Test
        @DisplayName("Future week should not be recognized as ended")
        void futureWeekIsNotEnded() {
            assertFalse(AllocationAdjustmentPolicy.isWeekEnded(futureWeek, today));
        }
    }

    @Nested
    @DisplayName("TC-02: validateCanRemove Tests")
    class ValidateCanRemoveTests {

        @Test
        @DisplayName("Should throw CannotRemoveAllocationWithActualHoursException if week ended AND has actual hours")
        void shouldThrowExceptionWhenWeekEndedAndHasActualHours() {
            CannotRemoveAllocationWithActualHoursException ex = assertThrows(
                    CannotRemoveAllocationWithActualHoursException.class,
                    () -> AllocationAdjustmentPolicy.validateCanRemove(100L, pastWeek, true, today)
            );
            assertEquals(100L, ex.getAllocationId());
            assertEquals(2026, ex.getYear());
            assertEquals(1, ex.getWeekNumber());
        }

        @Test
        @DisplayName("Should allow removal if week ended but has NO actual hours")
        void shouldAllowWhenWeekEndedButNoActualHours() {
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateCanRemove(100L, pastWeek, false, today));
        }

        @Test
        @DisplayName("Should allow removal if week is in the future even if actual hours flag is true")
        void shouldAllowWhenWeekInFuture() {
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateCanRemove(100L, futureWeek, true, today));
        }
    }

    @Nested
    @DisplayName("validateTargetWeek Tests")
    class ValidateTargetWeekTests {

        @Test
        @DisplayName("Should throw if targetWeek is null")
        void shouldThrowWhenTargetWeekIsNull() {
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateTargetWeek(currentWeek, null, today));
        }

        @Test
        @DisplayName("Should throw if targetWeek is identical to currentWeek")
        void shouldThrowWhenTargetWeekSameAsCurrentWeek() {
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateTargetWeek(currentWeek, currentWeek, today));
        }

        @Test
        @DisplayName("Should throw if targetWeek has already ended")
        void shouldThrowWhenTargetWeekHasEnded() {
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateTargetWeek(currentWeek, pastWeek, today));
        }

        @Test
        @DisplayName("Should pass when targetWeek is a valid future week")
        void shouldPassWhenTargetWeekIsValid() {
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateTargetWeek(currentWeek, futureWeek, today));
        }
    }

    @Nested
    @DisplayName("validateNewHours Tests")
    class ValidateNewHoursTests {

        @Test
        @DisplayName("Should throw when hours is null, zero, negative or > 168")
        void invalidHoursShouldThrow() {
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateNewHours(null));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateNewHours(BigDecimal.ZERO));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateNewHours(BigDecimal.valueOf(-5)));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateNewHours(BigDecimal.valueOf(169)));
        }

        @Test
        @DisplayName("Should pass when hours is valid")
        void validHoursShouldPass() {
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateNewHours(BigDecimal.valueOf(10)));
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateNewHours(BigDecimal.valueOf(40)));
        }
    }

    @Nested
    @DisplayName("validateAllocationPercentage Tests")
    class ValidateAllocationPercentageTests {

        @Test
        @DisplayName("Should throw when percentage is null, zero, negative or > 100")
        void invalidPercentageShouldThrow() {
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateAllocationPercentage(null));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateAllocationPercentage(BigDecimal.ZERO));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateAllocationPercentage(BigDecimal.valueOf(-10)));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateAllocationPercentage(BigDecimal.valueOf(101)));
        }

        @Test
        @DisplayName("Should pass when percentage is valid (> 0 and <= 100)")
        void validPercentageShouldPass() {
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateAllocationPercentage(BigDecimal.valueOf(0.01)));
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateAllocationPercentage(BigDecimal.valueOf(50)));
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateAllocationPercentage(BigDecimal.valueOf(100)));
        }
    }

    @Nested
    @DisplayName("validateVarianceNote Tests")
    class ValidateVarianceNoteTests {

        @Test
        @DisplayName("Should throw when note is null or empty or whitespace")
        void invalidNoteShouldThrow() {
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateVarianceNote(null));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateVarianceNote(""));
            assertThrows(InvalidAllocationAdjustmentException.class,
                    () -> AllocationAdjustmentPolicy.validateVarianceNote("   "));
        }

        @Test
        @DisplayName("Should pass when note has valid content")
        void validNoteShouldPass() {
            assertDoesNotThrow(() -> AllocationAdjustmentPolicy.validateVarianceNote("Nghỉ ốm đột xuất"));
        }
    }
}
