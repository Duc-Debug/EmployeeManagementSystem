package com.hrm.employeemanagement.domain.timesheet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException;
/**
 * Entity: Chấm giờ công việc
 * Timesheet
 */
public class Timesheet {

    private TimesheetId id;
    private final EmployeeId employeeId;
    private final LocalDate weekStartDate;
    private final LocalDate weekEndDate;
    private BigDecimal totalHours;
    private TimesheetStatus status;
    private LocalDateTime submittedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    private final List<TimesheetEntry> entries;

    public Timesheet(
            TimesheetId id,
            EmployeeId employeeId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            BigDecimal totalHours,
            TimesheetStatus status,
            LocalDateTime submittedAt,
            Long approvedBy,
            LocalDateTime approvedAt,
            String rejectionReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version,
            List<TimesheetEntry> entries) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.weekStartDate = Objects.requireNonNull(weekStartDate, "weekStartDate must not be null");
        this.weekEndDate = Objects.requireNonNull(weekEndDate, "weekEndDate must not be null");
        this.totalHours = totalHours != null ? totalHours : BigDecimal.ZERO;
        this.status = status != null ? status : TimesheetStatus.DRAFT;
        this.submittedAt = submittedAt;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    public static Timesheet create(EmployeeId employeeId, LocalDate dateInWeek) {
        LocalDate monday = dateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = dateInWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new Timesheet(
                null,
                employeeId,
                monday,
                sunday,
                BigDecimal.ZERO,
                TimesheetStatus.DRAFT,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                null,
                0L,
                new ArrayList<>()
        );
    }

    public void assertModifiable() {
        if (this.status != TimesheetStatus.DRAFT) {
            throw new TimesheetImmutableException("Bảng chấm công ở trạng thái [" + this.status + "] không thể sửa hoặc thêm dòng mới.");
        }
    }

    public void recalculateTotalHours() {
        this.totalHours = entries.stream()
                .map(TimesheetEntry::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.updatedAt = LocalDateTime.now();
    }

    public void addEntry(TimesheetEntry entry) {
        assertModifiable();
        if (this.id != null) {
            entry.assignTimesheetId(this.id);
        }
        this.entries.add(entry);
        recalculateTotalHours();
    }

    public void removeEntry(TimesheetEntryId entryId) {
        assertModifiable();
        this.entries.removeIf(e -> Objects.equals(e.getId(), entryId));
        recalculateTotalHours();
    }

    // Getters
    public TimesheetId getId() { return id; }
    public Long getIdValue() { return id != null ? id.value() : null; }
    public void setId(TimesheetId id) { this.id = id; }
    public EmployeeId getEmployeeId() { return employeeId; }
    public Long getEmployeeIdValue() { return employeeId != null ? employeeId.value() : null; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public LocalDate getWeekEndDate() { return weekEndDate; }
    public BigDecimal getTotalHours() { return totalHours; }
    public TimesheetStatus getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Long getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<TimesheetEntry> getEntries() { return Collections.unmodifiableList(entries); }
}
