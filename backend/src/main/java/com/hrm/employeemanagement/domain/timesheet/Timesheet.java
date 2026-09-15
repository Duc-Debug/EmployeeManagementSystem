package com.hrm.employeemanagement.domain.timesheet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.EmptyTimesheetSubmissionException;
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
    private LocalDateTime remindedAt;
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
        if (this.status != TimesheetStatus.DRAFT && this.status != TimesheetStatus.REJECTED) {
            throw new TimesheetImmutableException("Bảng chấm công ở trạng thái [" + this.status + "] không thể sửa hoặc thêm dòng mới.");
        }
    }

    public void submit() {
        if (this.status != TimesheetStatus.DRAFT && this.status != TimesheetStatus.REJECTED) {
            throw new TimesheetImmutableException("Chỉ có thể nộp bảng chấm công ở trạng thái nháp (DRAFT) hoặc bị từ chối (REJECTED).");
        }
        if (this.entries == null || this.entries.isEmpty()) {
            throw new EmptyTimesheetSubmissionException("Không thể nộp bảng chấm công rỗng. Vui lòng ghi nhận ít nhất một dòng giờ công.");
        }

        // QTN-09: Kiểm tra giới hạn 12 giờ/ngày cho tất cả các ngày trong tuần
        Map<LocalDate, BigDecimal> dailyTotals = this.entries.stream()
                .filter(e -> e.getWorkDate() != null && e.getHours() != null)
                .collect(Collectors.groupingBy(
                        TimesheetEntry::getWorkDate,
                        Collectors.reducing(BigDecimal.ZERO, TimesheetEntry::getHours, BigDecimal::add)
                ));

        for (Map.Entry<LocalDate, BigDecimal> entry : dailyTotals.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.valueOf(12)) > 0) {
                throw new DailyHoursLimitExceededException(entry.getKey(), BigDecimal.ZERO, entry.getValue());
            }
        }

        this.status = TimesheetStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.recalculateTotalHours();
        for (TimesheetEntry entry : this.entries) {
            entry.markSubmitted();
        }
    }

    public void setEntries(List<TimesheetEntry> newEntries) {
        this.entries.clear();
        if (newEntries != null) {
            this.entries.addAll(newEntries);
        }
        recalculateTotalHours();
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
    public LocalDateTime getRemindedAt() { return remindedAt; }
    public void setRemindedAt(LocalDateTime remindedAt) { this.remindedAt = remindedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<TimesheetEntry> getEntries() { return Collections.unmodifiableList(entries); }
}
