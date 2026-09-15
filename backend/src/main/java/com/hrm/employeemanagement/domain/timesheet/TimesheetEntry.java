package com.hrm.employeemanagement.domain.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogDescriptionBlankException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
/**
 *  Dòng ghi giờ công chi tiết, đại diện ghi nhận giờ thực tế trong 1 ngày.
 * TimesheetEntry
 */
public class TimesheetEntry {

    private TimesheetEntryId id;
    private TimesheetId timesheetId;
    private final EmployeeId employeeId;
    private ProjectId projectId;
    private TaskId taskId;
    private LocalDate workDate;
    private BigDecimal hours;
    private boolean billable;
    private String description;
    private TimesheetStatus status;
    private String rejectionReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public TimesheetEntry(
            TimesheetEntryId id,
            TimesheetId timesheetId,
            EmployeeId employeeId,
            ProjectId projectId,
            TaskId taskId,
            LocalDate workDate,
            BigDecimal hours,
            boolean billable,
            String description,
            TimesheetStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.id = id;
        this.timesheetId = timesheetId;
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.taskId = Objects.requireNonNull(taskId, "taskId must not be null");
        this.workDate = Objects.requireNonNull(workDate, "workDate must not be null");
        validateHours(hours);
        this.hours = hours;
        this.billable = billable;
        validateDescription(description);
        this.description = description.trim();
        this.status = status != null ? status : TimesheetStatus.DRAFT;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static TimesheetEntry create(
            TimesheetId timesheetId,
            EmployeeId employeeId,
            ProjectId projectId,
            TaskId taskId,
            LocalDate workDate,
            BigDecimal hours,
            boolean billable,
            String description) {
        return new TimesheetEntry(
                null,
                timesheetId,
                employeeId,
                projectId,
                taskId,
                workDate,
                hours,
                billable,
                description,
                TimesheetStatus.DRAFT,
                LocalDateTime.now(),
                null,
                0L
        );
    }

    public static void validateHours(BigDecimal hours) {
        if (hours == null) {
            throw new WorkLogInvalidHoursException("Số giờ làm việc không được để trống.");
        }
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WorkLogInvalidHoursException("Số giờ làm việc phải lớn hơn 0.");
        }
        if (hours.compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new WorkLogInvalidHoursException("Số giờ làm việc cho một mục không được vượt quá 24 giờ.");
        }
    }

    public static void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new WorkLogDescriptionBlankException("Mô tả nội dung công việc không được để trống.");
        }
    }

    public void updateDetails(
            ProjectId projectId,
            TaskId taskId,
            LocalDate workDate,
            BigDecimal hours,
            boolean billable,
            String description) {
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.taskId = Objects.requireNonNull(taskId, "taskId must not be null");
        this.workDate = Objects.requireNonNull(workDate, "workDate must not be null");
        validateHours(hours);
        this.hours = hours;
        this.billable = billable;
        validateDescription(description);
        this.description = description.trim();
        if (this.status == TimesheetStatus.REJECTED) {
            this.status = TimesheetStatus.DRAFT;
            this.rejectionReason = null;
        }
        touch();
    }

    public void assignTimesheetId(TimesheetId timesheetId) {
        this.timesheetId = Objects.requireNonNull(timesheetId, "timesheetId must not be null");
    }

    public boolean isDraft() {
        return this.status == TimesheetStatus.DRAFT;
    }

    public void assertModifiable() {
        if (this.status != TimesheetStatus.DRAFT && this.status != TimesheetStatus.REJECTED) {
            throw new TimesheetImmutableException("Dòng ghi giờ công ở trạng thái [" + this.status + "] không thể sửa hoặc xóa.");
        }
    }

    public void markSubmitted() {
        if (this.status != TimesheetStatus.DRAFT && this.status != TimesheetStatus.REJECTED) {
            throw new TimesheetImmutableException("Chỉ có thể nộp dòng giờ công ở trạng thái nháp hoặc bị từ chối.");
        }
        this.status = TimesheetStatus.SUBMITTED;
        this.rejectionReason = null;
        touch();
    }

    public void approve() {
        if (this.status != TimesheetStatus.SUBMITTED) {
            throw new TimesheetImmutableException("Chỉ có thể duyệt dòng giờ công đang ở trạng thái chờ duyệt (SUBMITTED).");
        }
        this.status = TimesheetStatus.APPROVED;
        this.rejectionReason = null;
        touch();
    }

    public void reject(String reason) {
        if (this.status != TimesheetStatus.SUBMITTED) {
            throw new TimesheetImmutableException("Chỉ có thể từ chối dòng giờ công đang ở trạng thái chờ duyệt (SUBMITTED).");
        }
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("Lý do từ chối không được để trống.");
        }
        this.status = TimesheetStatus.REJECTED;
        this.rejectionReason = reason.trim();
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
    // Getters
    public TimesheetEntryId getId() { return id; }
    public Long getIdValue() { return id != null ? id.value() : null; }
    public TimesheetId getTimesheetId() { return timesheetId; }
    public Long getTimesheetIdValue() { return timesheetId != null ? timesheetId.value() : null; }
    public EmployeeId getEmployeeId() { return employeeId; }
    public Long getEmployeeIdValue() { return employeeId != null ? employeeId.value() : null; }
    public ProjectId getProjectId() { return projectId; }
    public Long getProjectIdValue() { return projectId != null ? projectId.value() : null; }
    public TaskId getTaskId() { return taskId; }
    public Long getTaskIdValue() { return taskId != null ? taskId.value() : null; }
    public LocalDate getWorkDate() { return workDate; }
    public BigDecimal getHours() { return hours; }
    public boolean isBillable() { return billable; }
    public String getDescription() { return description; }
    public TimesheetStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
