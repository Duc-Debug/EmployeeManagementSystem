package com.hrm.employeemanagement.domain.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Entity đại diện cho Đơn nghỉ phép (NCL-05-CN-002, NCL-05-CN-003).
 */
public class LeaveRequest {

    private final Long id;
    private final Long employeeId;
    private final LeaveType leaveType;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int daysCount;
    private final BigDecimal hoursDeducted;
    private final String reason;
    private LeaveStatus status;
    private Long approverId;
    private String approverComment;
    private String cancellationReason;
    private LocalDateTime cancellationRequestedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LeaveRequest(Long id, Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
                        int daysCount, BigDecimal hoursDeducted, String reason, LeaveStatus status,
                        Long approverId, String approverComment,
                        String cancellationReason, LocalDateTime cancellationRequestedAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.leaveType = Objects.requireNonNull(leaveType, "leaveType must not be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        this.daysCount = daysCount;
        this.hoursDeducted = Objects.requireNonNull(hoursDeducted, "hoursDeducted must not be null");
        this.reason = reason;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.approverId = approverId;
        this.approverComment = approverComment;
        this.cancellationReason = cancellationReason;
        this.cancellationRequestedAt = cancellationRequestedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public LeaveRequest(Long id, Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
                        int daysCount, BigDecimal hoursDeducted, String reason, LeaveStatus status,
                        Long approverId, String approverComment,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, employeeId, leaveType, startDate, endDate, daysCount, hoursDeducted, reason, status, approverId, approverComment, null, null, createdAt, updatedAt);
    }

    /**
     * Constructor tương thích ngược cho các phần code hiện có.
     */
    public LeaveRequest(Long id, Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
                        int daysCount, BigDecimal hoursDeducted, String reason, LeaveStatus status,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, employeeId, leaveType, startDate, endDate, daysCount, hoursDeducted, reason, status, null, null, null, null, createdAt, updatedAt);
    }

    /**
     * Factory method tạo đơn xin nghỉ mới ở trạng thái PENDING (TC-01 của CN-002).
     */
    public static LeaveRequest createPending(Long employeeId, LeaveType leaveType,
                                             LocalDate startDate, LocalDate endDate,
                                             int daysCount, BigDecimal hoursDeducted,
                                             String reason) {
        LeaveRequestPolicy.validateDateRange(startDate, endDate);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do nghỉ phép không được để trống");
        }
        return new LeaveRequest(
                null,
                employeeId,
                leaveType,
                startDate,
                endDate,
                daysCount,
                hoursDeducted,
                reason,
                LeaveStatus.PENDING,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    /**
     * NCL-05-CN-003: Phê duyệt đơn xin nghỉ phép.
     * Quy tắc nghiệp vụ: Chỉ đơn ở trạng thái PENDING mới được phép phê duyệt.
     */
    public void approve(Long approverId, String comment) {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể phê duyệt đơn nghỉ phép khi đang ở trạng thái Chờ duyệt (PENDING)");
        }
        this.approverId = Objects.requireNonNull(approverId, "approverId must not be null");
        this.approverComment = comment;
        this.status = LeaveStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * NCL-05-CN-003: Từ chối đơn xin nghỉ phép.
     * Quy tắc nghiệp vụ: Chỉ đơn ở trạng thái PENDING mới được phép từ chối, và bắt buộc phải có lý do.
     */
    public void reject(Long approverId, String rejectionReason) {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối đơn nghỉ phép khi đang ở trạng thái Chờ duyệt (PENDING)");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do từ chối đơn nghỉ phép không được để trống");
        }
        this.approverId = Objects.requireNonNull(approverId, "approverId must not be null");
        this.approverComment = rejectionReason.trim();
        this.status = LeaveStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hủy đơn xin nghỉ phép khi còn ở trạng thái PENDING (dành cho nhân viên).
     */
    public void cancel() {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn nghỉ phép khi đang ở trạng thái Chờ duyệt (PENDING)");
        }
        this.status = LeaveStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * NCL-05-CN-007: Nhân viên gửi yêu cầu hủy đơn nghỉ phép đã duyệt.
     * Quy tắc nghiệp vụ: Đơn phải đang ở trạng thái APPROVED và ngày nghỉ chưa diễn ra (startDate > today).
     */
    public void requestCancellation(String reason, LocalDate today) {
        if (this.status != LeaveStatus.APPROVED) {
            throw new IllegalStateException("Chỉ có thể yêu cầu hủy đơn nghỉ phép khi đơn đã được duyệt (APPROVED)");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do yêu cầu hủy đơn nghỉ phép không được để trống");
        }
        LeaveRequestPolicy.validateLeaveNotStarted(this.startDate, today);

        this.status = LeaveStatus.CANCEL_REQUESTED;
        this.cancellationReason = reason.trim();
        this.cancellationRequestedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * NCL-05-CN-007: Quản lý nguồn lực phê duyệt yêu cầu hủy đơn nghỉ phép.
     * Quy tắc nghiệp vụ: Đơn phải đang ở trạng thái CANCEL_REQUESTED và ngày nghỉ chưa diễn ra.
     */
    public void approveCancellation(Long approverId, String comment, LocalDate today) {
        if (this.status != LeaveStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("Chỉ có thể duyệt hủy khi đơn đang ở trạng thái Chờ duyệt hủy (CANCEL_REQUESTED)");
        }
        LeaveRequestPolicy.validateLeaveNotStarted(this.startDate, today);

        this.approverId = Objects.requireNonNull(approverId, "approverId must not be null");
        if (comment != null && !comment.trim().isEmpty()) {
            this.approverComment = comment.trim();
        }
        this.status = LeaveStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * NCL-05-CN-007: Quản lý nguồn lực từ chối yêu cầu hủy đơn nghỉ phép.
     * Quy tắc nghiệp vụ: Đơn quay trở lại trạng thái APPROVED.
     */
    public void rejectCancellation(Long approverId, String rejectionReason) {
        if (this.status != LeaveStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("Chỉ có thể từ chối hủy khi đơn đang ở trạng thái Chờ duyệt hủy (CANCEL_REQUESTED)");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do từ chối yêu cầu hủy không được để trống");
        }
        this.approverId = Objects.requireNonNull(approverId, "approverId must not be null");
        this.approverComment = rejectionReason.trim();
        this.status = LeaveStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public LeaveType getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getDaysCount() { return daysCount; }
    public BigDecimal getHoursDeducted() { return hoursDeducted; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }
    public Long getApproverId() { return approverId; }
    public String getApproverComment() { return approverComment; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCancellationRequestedAt() { return cancellationRequestedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
