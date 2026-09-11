package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "leave_type", nullable = false)
    private String leaveType; // ANNUAL, UNPAID, SICK, PERSONAL

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    @Column(name = "hours_deducted", nullable = false, precision = 5, scale = 2)
    private BigDecimal hoursDeducted;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approver_comment", length = 500)
    private String approverComment;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public LeaveRequestJpaEntity() {}

    public LeaveRequestJpaEntity(Long id, Long employeeId, LocalDate startDate, LocalDate endDate,
                                 String status, BigDecimal hoursDeducted) {
        this(id, employeeId, "ANNUAL", startDate, endDate, status, hoursDeducted, null, null, null, null, null);
    }

    public LeaveRequestJpaEntity(Long id, Long employeeId, String leaveType, LocalDate startDate, LocalDate endDate,
                                 String status, BigDecimal hoursDeducted, String reason,
                                 Long approverId, String approverComment,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.hoursDeducted = hoursDeducted;
        this.reason = reason;
        this.approverId = approverId;
        this.approverComment = approverComment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getHoursDeducted() {
        return hoursDeducted;
    }

    public void setHoursDeducted(BigDecimal hoursDeducted) {
        this.hoursDeducted = hoursDeducted;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getApproverComment() {
        return approverComment;
    }

    public void setApproverComment(String approverComment) {
        this.approverComment = approverComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
