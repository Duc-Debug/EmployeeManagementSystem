package com.hrm.employeemanagement.domain.allocation.confirmation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScheduleConfirmationPolicyTest {

    @Test
    @DisplayName("normalizeToMonday: Chuẩn hóa mọi ngày trong tuần về đúng Thứ Hai gần nhất")
    void testNormalizeToMonday() {
        LocalDate wednesday = LocalDate.of(2026, 9, 23);
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDate sunday = LocalDate.of(2026, 9, 27);

        assertThat(ScheduleConfirmationPolicy.normalizeToMonday(wednesday)).isEqualTo(monday);
        assertThat(ScheduleConfirmationPolicy.normalizeToMonday(monday)).isEqualTo(monday);
        assertThat(ScheduleConfirmationPolicy.normalizeToMonday(sunday)).isEqualTo(monday);
    }

    @Test
    @DisplayName("TC-03: clampWeeks kiểm thử toàn diện các mốc biên boundary: null, -99, -1, 0, 1, 5, 8, 9, 99")
    void testClampWeeksBoundaries() {
        assertThat(ScheduleConfirmationPolicy.clampWeeks(null)).isEqualTo(2);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(-99)).isEqualTo(1);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(-1)).isEqualTo(1);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(0)).isEqualTo(1);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(1)).isEqualTo(1);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(5)).isEqualTo(5);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(8)).isEqualTo(8);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(9)).isEqualTo(8);
        assertThat(ScheduleConfirmationPolicy.clampWeeks(99)).isEqualTo(8);
    }

    @Test
    @DisplayName("determineConfirmationStatus: NOT_CONFIRMED khi chưa có confirmedAt")
    void testStatusNotConfirmed() {
        LocalDateTime maxUpdated = LocalDateTime.of(2026, 9, 18, 10, 0);
        assertThat(ScheduleConfirmationPolicy.determineConfirmationStatus(null, maxUpdated))
                .isEqualTo(ConfirmationStatus.NOT_CONFIRMED);
    }

    @Test
    @DisplayName("determineConfirmationStatus: CONFIRMED khi confirmedAt >= maxUpdated hoặc không có phân bổ")
    void testStatusConfirmed() {
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 9, 18, 12, 0);
        LocalDateTime maxUpdated = LocalDateTime.of(2026, 9, 18, 10, 0);

        assertThat(ScheduleConfirmationPolicy.determineConfirmationStatus(confirmedAt, maxUpdated))
                .isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(ScheduleConfirmationPolicy.determineConfirmationStatus(confirmedAt, confirmedAt))
                .isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(ScheduleConfirmationPolicy.determineConfirmationStatus(confirmedAt, null))
                .isEqualTo(ConfirmationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("determineConfirmationStatus: STALE khi maxUpdated > confirmedAt")
    void testStatusStale() {
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 9, 18, 10, 0);
        LocalDateTime maxUpdated = LocalDateTime.of(2026, 9, 18, 11, 0);

        assertThat(ScheduleConfirmationPolicy.determineConfirmationStatus(confirmedAt, maxUpdated))
                .isEqualTo(ConfirmationStatus.STALE);
    }

    @Test
    @DisplayName("evaluateConfirmationAction: Xác nhận lần đầu trả HTTP 201 Created")
    void testActionFirstTime() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 10, 0);
        var result = ScheduleConfirmationPolicy.evaluateConfirmationAction(null, null, now);

        assertThat(result.httpStatusCode()).isEqualTo(201);
        assertThat(result.alreadyConfirmed()).isFalse();
        assertThat(result.previousConfirmationWasStale()).isFalse();
        assertThat(result.confirmedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("evaluateConfirmationAction: Double-click giữ nguyên confirmed_at và trả HTTP 200 already_confirmed=true")
    void testActionDoubleClick() {
        LocalDateTime oldConfirmed = LocalDateTime.of(2026, 9, 18, 10, 0);
        LocalDateTime maxUpdated = LocalDateTime.of(2026, 9, 18, 9, 0);
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 12, 0);

        var result = ScheduleConfirmationPolicy.evaluateConfirmationAction(oldConfirmed, maxUpdated, now);

        assertThat(result.httpStatusCode()).isEqualTo(200);
        assertThat(result.alreadyConfirmed()).isTrue();
        assertThat(result.previousConfirmationWasStale()).isFalse();
        assertThat(result.confirmedAt()).isEqualTo(oldConfirmed);
    }

    @Test
    @DisplayName("evaluateConfirmationAction: Xác nhận lại khi STALE trả HTTP 200 already_confirmed=false, previous_was_stale=true")
    void testActionReconfirmStale() {
        LocalDateTime oldConfirmed = LocalDateTime.of(2026, 9, 18, 10, 0);
        LocalDateTime maxUpdated = LocalDateTime.of(2026, 9, 18, 11, 0);
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 12, 0);

        var result = ScheduleConfirmationPolicy.evaluateConfirmationAction(oldConfirmed, maxUpdated, now);

        assertThat(result.httpStatusCode()).isEqualTo(200);
        assertThat(result.alreadyConfirmed()).isFalse();
        assertThat(result.previousConfirmationWasStale()).isTrue();
        assertThat(result.confirmedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("WeeklyAllocationCalculator: Tính toán tổng giờ và tìm maxUpdatedAt chính xác")
    void testWeeklyAllocationCalculator() {
        assertThat(WeeklyAllocationCalculator.calculateTotalHours(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(WeeklyAllocationCalculator.calculateTotalHours(List.of())).isEqualTo(new BigDecimal("0.00"));

        LocalDateTime t1 = LocalDateTime.of(2026, 9, 18, 8, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 9, 18, 9, 30);
        AllocationItem item1 = new AllocationItem(1L, 10L, "Project A", "ACTIVE", new BigDecimal("15.50"), t1);
        AllocationItem item2 = new AllocationItem(2L, 20L, "Project B", "ACTIVE", new BigDecimal("24.50"), t2);

        assertThat(WeeklyAllocationCalculator.calculateTotalHours(List.of(item1, item2))).isEqualTo(new BigDecimal("40.00"));
        assertThat(WeeklyAllocationCalculator.findMaxUpdatedAt(List.of(item1, item2))).isEqualTo(t2);
    }

    @Test
    @DisplayName("WeeklyScheduleAggregate: Thực thi các Invariants nghiêm ngặt")
    void testWeeklyScheduleAggregateInvariants() {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDate tuesday = LocalDate.of(2026, 9, 22);

        AllocationItem item = new AllocationItem(1L, 10L, "Project A", "ACTIVE", new BigDecimal("40.00"), LocalDateTime.now());

        // Invariant: Non-Monday throws exception
        assertThatThrownBy(() -> new WeeklyScheduleAggregate(tuesday, null, List.of(item)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phải là Thứ Hai");

        // Valid Aggregate
        WeeklyScheduleAggregate agg = new WeeklyScheduleAggregate(monday, null, List.of(item));
        assertThat(agg.getWeekStartDate()).isEqualTo(monday);
        assertThat(agg.getTotalHours()).isEqualTo(new BigDecimal("40.00"));
        assertThat(agg.getConfirmationStatus()).isEqualTo(ConfirmationStatus.NOT_CONFIRMED);
    }
}