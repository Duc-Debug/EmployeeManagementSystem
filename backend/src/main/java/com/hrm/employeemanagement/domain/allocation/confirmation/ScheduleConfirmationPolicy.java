package com.hrm.employeemanagement.domain.allocation.confirmation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Domain Policy chuyên trách về trạng thái xác nhận và chuyển đổi trạng thái:
 * - Chuẩn hóa ngày về Thứ Hai (Monday)
 * - Ép dải số tuần (clamping [1, 8])
 * - Đánh giá trạng thái xác nhận (NOT_CONFIRMED, CONFIRMED, STALE, HAS_FEEDBACK)
 * - Xác định kết quả hành động xác nhận (first-time 201, idempotent 200, re-confirm stale 200)
 * - Nghiệp vụ QTN-24: Ghi nhận ý kiến phản hồi kèm lý do bắt buộc mà không làm tự động thay đổi phân bổ.
 */
public final class ScheduleConfirmationPolicy {

    public static final int DEFAULT_WEEKS = 2;
    public static final int MIN_WEEKS = 1;
    public static final int MAX_WEEKS = 8;

    private ScheduleConfirmationPolicy() {
    }

    public static LocalDate normalizeToMonday(LocalDate date) {
        Objects.requireNonNull(date, "Date must not be null");
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static int clampWeeks(Integer weeks) {
        if (weeks == null) {
            return DEFAULT_WEEKS;
        }
        return Math.max(MIN_WEEKS, Math.min(MAX_WEEKS, weeks));
    }

    public static ConfirmationStatus determineConfirmationStatus(
            LocalDateTime confirmedAt,
            LocalDateTime feedbackAt,
            String feedbackNote,
            LocalDateTime maxAllocationUpdatedAt) {
        boolean hasFeedback = feedbackNote != null && !feedbackNote.isBlank() && feedbackAt != null;

        if (hasFeedback && (confirmedAt == null || feedbackAt.isAfter(confirmedAt))) {
            if (maxAllocationUpdatedAt != null && maxAllocationUpdatedAt.isAfter(feedbackAt)) {
                return ConfirmationStatus.STALE;
            }
            return ConfirmationStatus.HAS_FEEDBACK;
        }

        if (confirmedAt == null) {
            return ConfirmationStatus.NOT_CONFIRMED;
        }
        if (maxAllocationUpdatedAt != null && maxAllocationUpdatedAt.isAfter(confirmedAt)) {
            return ConfirmationStatus.STALE;
        }
        return ConfirmationStatus.CONFIRMED;
    }

    public static ConfirmationStatus determineConfirmationStatus(LocalDateTime confirmedAt, LocalDateTime maxAllocationUpdatedAt) {
        return determineConfirmationStatus(confirmedAt, null, null, maxAllocationUpdatedAt);
    }

    public record ConfirmationActionResult(
            int httpStatusCode,
            boolean alreadyConfirmed,
            boolean previousConfirmationWasStale,
            LocalDateTime confirmedAt
    ) {}

    public static ConfirmationActionResult evaluateConfirmationAction(
            LocalDateTime currentConfirmedAt,
            LocalDateTime maxAllocationUpdatedAt,
            LocalDateTime now
    ) {
        Objects.requireNonNull(now, "Now timestamp must not be null");

        if (currentConfirmedAt == null) {
            // Xác nhận lần đầu: 201 Created
            return new ConfirmationActionResult(201, false, false, now);
        }

        boolean isStale = maxAllocationUpdatedAt != null && maxAllocationUpdatedAt.isAfter(currentConfirmedAt);
        if (isStale) {
            // Xác nhận lại sau khi STALE: 200 OK, previous_confirmation_was_stale: true
            return new ConfirmationActionResult(200, false, true, now);
        }

        // Double click / Retry khi dữ liệu không đổi: 200 OK, already_confirmed: true, giữ confirmed_at cũ
        return new ConfirmationActionResult(200, true, false, currentConfirmedAt);
    }

    public static void validateFeedbackReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do hoặc ý kiến phản hồi không được để trống theo quy định QTN-24");
        }
    }
}
