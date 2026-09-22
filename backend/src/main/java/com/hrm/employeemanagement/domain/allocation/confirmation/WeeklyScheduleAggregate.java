package com.hrm.employeemanagement.domain.allocation.confirmation;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain Aggregate đại diện cho Lịch phân bổ của 1 tuần cụ thể.
 * Encapsulate các invariants nghiệp vụ:
 * 1. weekStartDate luôn phải là Thứ Hai (Monday).
 * 2. totalHours luôn phản ánh chính xác tổng giờ của danh sách allocations.
 * 3. confirmationStatus tuân thủ quy tắc đánh giá của domain policy (CONFIRMED, STALE, HAS_FEEDBACK, NOT_CONFIRMED).
 */
public class WeeklyScheduleAggregate {

    private final LocalDate weekStartDate;
    private final BigDecimal totalHours;
    private final ConfirmationStatus confirmationStatus;
    private final LocalDateTime confirmedAt;
    private final String feedbackNote;
    private final LocalDateTime feedbackAt;
    private final List<AllocationItem> allocations;

    public WeeklyScheduleAggregate(
            LocalDate weekStartDate,
            LocalDateTime confirmedAt,
            String feedbackNote,
            LocalDateTime feedbackAt,
            List<AllocationItem> allocations) {
        Objects.requireNonNull(weekStartDate, "weekStartDate must not be null");
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStartDate phải là Thứ Hai (Monday), giá trị nhận được: " + weekStartDate);
        }

        this.weekStartDate = weekStartDate;
        this.allocations = allocations != null ? List.copyOf(allocations) : Collections.emptyList();
        this.totalHours = WeeklyAllocationCalculator.calculateTotalHours(this.allocations);
        this.confirmedAt = confirmedAt;
        this.feedbackNote = feedbackNote;
        this.feedbackAt = feedbackAt;

        LocalDateTime maxUpdatedAt = WeeklyAllocationCalculator.findMaxUpdatedAt(this.allocations);
        this.confirmationStatus = ScheduleConfirmationPolicy.determineConfirmationStatus(confirmedAt, feedbackAt, feedbackNote, maxUpdatedAt);
    }

    public WeeklyScheduleAggregate(
            LocalDate weekStartDate,
            LocalDateTime confirmedAt,
            List<AllocationItem> allocations) {
        this(weekStartDate, confirmedAt, null, null, allocations);
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public ConfirmationStatus getConfirmationStatus() {
        return confirmationStatus;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public String getFeedbackNote() {
        return feedbackNote;
    }

    public LocalDateTime getFeedbackAt() {
        return feedbackAt;
    }

    public List<AllocationItem> getAllocations() {
        return allocations;
    }
}
