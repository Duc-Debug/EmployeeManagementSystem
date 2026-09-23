package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "schedule_conflict_warnings")
public class ScheduleConflictJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false)
    private ConflictType conflictType;

    @Column(name = "project_ids")
    private String projectIds;

    @Column(name = "project_names", columnDefinition = "TEXT")
    private String projectNames;

    @Column(name = "leave_request_id")
    private Long leaveRequestId;

    @Column(name = "leave_info")
    private String leaveInfo;

    @Column(name = "total_allocated_hours", nullable = false)
    private BigDecimal totalAllocatedHours;

    @Column(name = "net_available_hours", nullable = false)
    private BigDecimal netAvailableHours;

    @Column(name = "excess_hours", nullable = false)
    private BigDecimal excessHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScheduleConflictStatus status;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "notified_by")
    private Long notifiedBy;

    @Column(name = "assigned_handler_id")
    private Long assignedHandlerId;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "is_recurrent")
    private Boolean isRecurrent = false;

    @Column(name = "recurrent_note", columnDefinition = "TEXT")
    private String recurrentNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public ScheduleConflictJpaEntity() {
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

    public Integer getYearNumber() {
        return yearNumber;
    }

    public void setYearNumber(Integer yearNumber) {
        this.yearNumber = yearNumber;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public void setConflictType(ConflictType conflictType) {
        this.conflictType = conflictType;
    }

    public String getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(String projectIds) {
        this.projectIds = projectIds;
    }

    public String getProjectNames() {
        return projectNames;
    }

    public void setProjectNames(String projectNames) {
        this.projectNames = projectNames;
    }

    public Long getLeaveRequestId() {
        return leaveRequestId;
    }

    public void setLeaveRequestId(Long leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
    }

    public String getLeaveInfo() {
        return leaveInfo;
    }

    public void setLeaveInfo(String leaveInfo) {
        this.leaveInfo = leaveInfo;
    }

    public BigDecimal getTotalAllocatedHours() {
        return totalAllocatedHours;
    }

    public void setTotalAllocatedHours(BigDecimal totalAllocatedHours) {
        this.totalAllocatedHours = totalAllocatedHours;
    }

    public BigDecimal getNetAvailableHours() {
        return netAvailableHours;
    }

    public void setNetAvailableHours(BigDecimal netAvailableHours) {
        this.netAvailableHours = netAvailableHours;
    }

    public BigDecimal getExcessHours() {
        return excessHours;
    }

    public void setExcessHours(BigDecimal excessHours) {
        this.excessHours = excessHours;
    }

    public ScheduleConflictStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleConflictStatus status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public Long getNotifiedBy() {
        return notifiedBy;
    }

    public void setNotifiedBy(Long notifiedBy) {
        this.notifiedBy = notifiedBy;
    }

    public Long getAssignedHandlerId() {
        return assignedHandlerId;
    }

    public void setAssignedHandlerId(Long assignedHandlerId) {
        this.assignedHandlerId = assignedHandlerId;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Boolean getIsRecurrent() {
        return isRecurrent;
    }

    public void setIsRecurrent(Boolean isRecurrent) {
        this.isRecurrent = isRecurrent != null ? isRecurrent : false;
    }

    public String getRecurrentNote() {
        return recurrentNote;
    }

    public void setRecurrentNote(String recurrentNote) {
        this.recurrentNote = recurrentNote;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
