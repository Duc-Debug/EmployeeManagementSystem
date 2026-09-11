package com.hrm.employeemanagement.domain.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Entity đại diện cho Đơn nghỉ phép (NCL-05-CN-002).
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
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LeaveRequest(Long id, Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
                        int daysCount, BigDecimal hoursDeducted, String reason, LeaveStatus status,
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method tạo đơn xin nghỉ mới ở trạng thái PENDING (TC-01).
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
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    /**
     * Hủy đơn xin nghỉ phép khi còn ở trạng thái PENDING.
     */
    public void cancel() {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn nghỉ phép khi đang ở trạng thái Chờ duyệt (PENDING)");
        }
        this.status = LeaveStatus.CANCELLED;
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
