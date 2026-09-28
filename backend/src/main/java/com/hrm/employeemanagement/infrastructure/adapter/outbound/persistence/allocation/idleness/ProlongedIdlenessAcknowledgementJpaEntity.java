package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.idleness;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prolonged_idleness_acknowledgements")
public class ProlongedIdlenessAcknowledgementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "from_year", nullable = false)
    private Integer fromYear;

    @Column(name = "from_week", nullable = false)
    private Integer fromWeek;

    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks;

    @Column(name = "action_taken", length = 500, nullable = false)
    private String actionTaken;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "ACKNOWLEDGED";

    @Column(name = "acknowledged_by", nullable = false)
    private Long acknowledgedBy;

    @Column(name = "acknowledged_at", nullable = false)
    private LocalDateTime acknowledgedAt = LocalDateTime.now();

    public ProlongedIdlenessAcknowledgementJpaEntity() {
    }

    public ProlongedIdlenessAcknowledgementJpaEntity(
            Long id,
            Long employeeId,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            String actionTaken,
            String notes,
            String status,
            Long acknowledgedBy,
            LocalDateTime acknowledgedAt
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.fromYear = fromYear;
        this.fromWeek = fromWeek;
        this.durationWeeks = durationWeeks;
        this.actionTaken = actionTaken;
        this.notes = notes;
        this.status = status;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = acknowledgedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getFromYear() {
        return fromYear;
    }

    public void setFromYear(Integer fromYear) {
        this.fromYear = fromYear;
    }

    public Integer getFromWeek() {
        return fromWeek;
    }

    public void setFromWeek(Integer fromWeek) {
        this.fromWeek = fromWeek;
    }

    public Integer getDurationWeeks() {
        return durationWeeks;
    }

    public void setDurationWeeks(Integer durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(Long acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
}
