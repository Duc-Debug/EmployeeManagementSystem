package com.hrm.employeemanagement.domain.allocation.idleness;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Entity ghi nhận xác nhận xử lý cảnh báo nhân sự nhàn rỗi kéo dài (NCL-07-CN-006-TC-04).
 */
public class ProlongedIdlenessAcknowledgement {

    private final Long id;
    private final Long employeeId;
    private final int fromYear;
    private final int fromWeek;
    private final int durationWeeks;
    private final String actionTaken;
    private final String notes;
    private final String status;
    private final Long acknowledgedBy;
    private final LocalDateTime acknowledgedAt;

    public ProlongedIdlenessAcknowledgement(
            Long id,
            Long employeeId,
            int fromYear,
            int fromWeek,
            int durationWeeks,
            String actionTaken,
            String notes,
            String status,
            Long acknowledgedBy,
            LocalDateTime acknowledgedAt
    ) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.fromYear = fromYear;
        this.fromWeek = fromWeek;
        this.durationWeeks = durationWeeks;
        this.actionTaken = Objects.requireNonNull(actionTaken, "actionTaken must not be null");
        this.notes = notes;
        this.status = status != null ? status : "ACKNOWLEDGED";
        this.acknowledgedBy = Objects.requireNonNull(acknowledgedBy, "acknowledgedBy must not be null");
        this.acknowledgedAt = acknowledgedAt != null ? acknowledgedAt : LocalDateTime.now();
    }

    public static ProlongedIdlenessAcknowledgement create(
            Long employeeId,
            int fromYear,
            int fromWeek,
            int durationWeeks,
            String actionTaken,
            String notes,
            Long acknowledgedBy
    ) {
        return new ProlongedIdlenessAcknowledgement(
                null,
                employeeId,
                fromYear,
                fromWeek,
                durationWeeks,
                actionTaken,
                notes,
                "ACKNOWLEDGED",
                acknowledgedBy,
                LocalDateTime.now()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public int getFromYear() {
        return fromYear;
    }

    public int getFromWeek() {
        return fromWeek;
    }

    public int getDurationWeeks() {
        return durationWeeks;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public Long getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
