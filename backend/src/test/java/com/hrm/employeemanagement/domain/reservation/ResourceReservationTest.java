package com.hrm.employeemanagement.domain.reservation;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationDataException;
import com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResourceReservation Domain Unit Tests (QTN-13)")
class ResourceReservationTest {

    private final YearWeek targetWeek = YearWeek.of(2026, 40);

    @Test
    @DisplayName("Khởi tạo thành công bản ghi giữ chỗ với dữ liệu hợp lệ")
    void shouldCreateNewReservationSuccessfully() {
        ResourceReservation reservation = ResourceReservation.createNew(
                10L, 20L, targetWeek, BigDecimal.valueOf(20.0), "Giữ chỗ dự án dự kiến", 1L
        );

        assertNotNull(reservation);
        assertNull(reservation.getId());
        assertEquals(10L, reservation.getProjectId());
        assertEquals(20L, reservation.getEmployeeId());
        assertEquals(2026, reservation.getYear());
        assertEquals(40, reservation.getWeekNumber());
        assertEquals(BigDecimal.valueOf(20.0), reservation.getReservedHours());
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
        assertTrue(reservation.isActive());
        assertFalse(reservation.isConverted());
        assertFalse(reservation.isCancelled());
        assertNull(reservation.getConvertedAllocationId());
        assertNull(reservation.getCancelledReason());
        assertEquals("Giữ chỗ dự án dự kiến", reservation.getNote());
        assertEquals(1L, reservation.getCreatedBy());
    }

    @Test
    @DisplayName("Ném InvalidReservationDataException khi số giờ <= 0")
    void shouldThrowWhenReservedHoursIsZeroOrNegative() {
        assertThrows(InvalidReservationDataException.class, () ->
                ResourceReservation.createNew(10L, 20L, targetWeek, BigDecimal.ZERO, "Note", 1L)
        );

        assertThrows(InvalidReservationDataException.class, () ->
                ResourceReservation.createNew(10L, 20L, targetWeek, BigDecimal.valueOf(-5.0), "Note", 1L)
        );
    }

    @Test
    @DisplayName("Ném InvalidReservationDataException khi số giờ vượt quá 168h/tuần")
    void shouldThrowWhenReservedHoursExceedsWeeklyMax() {
        assertThrows(InvalidReservationDataException.class, () ->
                ResourceReservation.createNew(10L, 20L, targetWeek, BigDecimal.valueOf(169.0), "Note", 1L)
        );
    }

    @Test
    @DisplayName("TC-02: Hủy giữ chỗ thành công khi đang ACTIVE")
    void shouldCancelActiveReservationSuccessfully() {
        ResourceReservation reservation = ResourceReservation.createNew(
                10L, 20L, targetWeek, BigDecimal.valueOf(20.0), "Note", 1L
        );

        reservation.cancel(2L, "Dự án bị hủy");

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertTrue(reservation.isCancelled());
        assertFalse(reservation.isActive());
        assertEquals("Dự án bị hủy", reservation.getCancelledReason());
        assertEquals(2L, reservation.getUpdatedBy());
        assertNotNull(reservation.getUpdatedAt());
    }

    @Test
    @DisplayName("TC-03: Chuyển đổi giữ chỗ thành phân bổ thành công khi đang ACTIVE")
    void shouldConvertActiveReservationSuccessfully() {
        ResourceReservation reservation = ResourceReservation.createNew(
                10L, 20L, targetWeek, BigDecimal.valueOf(20.0), "Note", 1L
        );

        reservation.convert(100L, 2L);

        assertEquals(ReservationStatus.CONVERTED, reservation.getStatus());
        assertTrue(reservation.isConverted());
        assertFalse(reservation.isActive());
        assertEquals(100L, reservation.getConvertedAllocationId());
        assertEquals(2L, reservation.getUpdatedBy());
        assertNotNull(reservation.getUpdatedAt());
    }

    @Test
    @DisplayName("Ném InvalidReservationStateException khi hủy một reservation đã CONVERTED")
    void shouldThrowWhenCancellingConvertedReservation() {
        ResourceReservation reservation = ResourceReservation.createNew(
                10L, 20L, targetWeek, BigDecimal.valueOf(20.0), "Note", 1L
        );
        reservation.convert(100L, 2L);

        assertThrows(InvalidReservationStateException.class, () ->
                reservation.cancel(3L, "Thử hủy khi đã duyệt")
        );
    }

    @Test
    @DisplayName("Ném InvalidReservationStateException khi chuyển đổi một reservation đã CANCELLED")
    void shouldThrowWhenConvertingCancelledReservation() {
        ResourceReservation reservation = ResourceReservation.createNew(
                10L, 20L, targetWeek, BigDecimal.valueOf(20.0), "Note", 1L
        );
        reservation.cancel(2L, "Dự án bị hủy");

        assertThrows(InvalidReservationStateException.class, () ->
                reservation.convert(100L, 3L)
        );
    }
}
