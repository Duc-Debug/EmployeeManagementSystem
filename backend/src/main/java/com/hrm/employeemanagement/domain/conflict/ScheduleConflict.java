package com.hrm.employeemanagement.domain.conflict;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class ScheduleConflict {

    private Long id;
    private Long employeeId;
    private Integer yearNumber;
    private Integer weekNumber;
    private ConflictType conflictType;
    private String projectIds;
    private String projectNames;
    private Long leaveRequestId;
    private String leaveInfo;
    private BigDecimal totalAllocatedHours;
    private BigDecimal netAvailableHours;
    private BigDecimal excessHours;
    private ScheduleConflictStatus status;
    private String details;
    private LocalDateTime notifiedAt;
    private Long notifiedBy;
    private Long assignedHandlerId;
    private String resolutionNote;
    private Boolean isRecurrent = false;
    private String recurrentNote;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public ScheduleConflict() {
    }

    public ScheduleConflict(
            Long id,
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            ConflictType conflictType,
            String projectIds,
            String projectNames,
            Long leaveRequestId,
            String leaveInfo,
            BigDecimal totalAllocatedHours,
            BigDecimal netAvailableHours,
            BigDecimal excessHours,
            ScheduleConflictStatus status,
            String details,
            LocalDateTime notifiedAt,
            Long notifiedBy,
            Long assignedHandlerId,
            String resolutionNote,
            Boolean isRecurrent,
            String recurrentNote,
            LocalDateTime resolvedAt,
            Long resolvedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, employeeId, yearNumber, weekNumber, conflictType, projectIds, projectNames,
                leaveRequestId, leaveInfo, totalAllocatedHours, netAvailableHours, excessHours,
                status, details, notifiedAt, notifiedBy, assignedHandlerId, resolutionNote,
                isRecurrent, recurrentNote, resolvedAt, resolvedBy, createdAt, updatedAt, 0L);
    }

    public ScheduleConflict(
            Long id,
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            ConflictType conflictType,
            String projectIds,
            String projectNames,
            Long leaveRequestId,
            String leaveInfo,
            BigDecimal totalAllocatedHours,
            BigDecimal netAvailableHours,
            BigDecimal excessHours,
            ScheduleConflictStatus status,
            String details,
            LocalDateTime notifiedAt,
            Long notifiedBy,
            Long assignedHandlerId,
            String resolutionNote,
            Boolean isRecurrent,
            String recurrentNote,
            LocalDateTime resolvedAt,
            Long resolvedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.yearNumber = Objects.requireNonNull(yearNumber, "yearNumber must not be null");
        this.weekNumber = Objects.requireNonNull(weekNumber, "weekNumber must not be null");
        this.conflictType = Objects.requireNonNull(conflictType, "conflictType must not be null");
        this.projectIds = projectIds;
        this.projectNames = projectNames;
        this.leaveRequestId = leaveRequestId;
        this.leaveInfo = leaveInfo;
        this.totalAllocatedHours = totalAllocatedHours != null ? totalAllocatedHours : BigDecimal.ZERO;
        this.netAvailableHours = netAvailableHours != null ? netAvailableHours : BigDecimal.ZERO;
        this.excessHours = excessHours != null ? excessHours : BigDecimal.ZERO;
        this.status = status != null ? status : ScheduleConflictStatus.OPEN;
        this.details = details;
        this.notifiedAt = notifiedAt;
        this.notifiedBy = notifiedBy;
        this.assignedHandlerId = assignedHandlerId;
        this.resolutionNote = resolutionNote;
        this.isRecurrent = isRecurrent != null ? isRecurrent : false;
        this.recurrentNote = recurrentNote;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static ScheduleConflict create(
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            ConflictType conflictType,
            String projectIds,
            String projectNames,
            Long leaveRequestId,
            String leaveInfo,
            BigDecimal totalAllocatedHours,
            BigDecimal netAvailableHours,
            BigDecimal excessHours,
            String details
    ) {
        return new ScheduleConflict(
                null,
                employeeId,
                yearNumber,
                weekNumber,
                conflictType,
                projectIds,
                projectNames,
                leaveRequestId,
                leaveInfo,
                totalAllocatedHours,
                netAvailableHours,
                excessHours,
                ScheduleConflictStatus.OPEN,
                details,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void markAsNotified(Long userId) {
        this.status = ScheduleConflictStatus.NOTIFIED;
        this.notifiedBy = userId;
        this.notifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsResolved() {
        markAsResolved(null);
    }

    public void markAsResolved(Long userId) {
        if (this.status == ScheduleConflictStatus.RESOLVED) {
            throw new IllegalStateException("Cảnh báo xung đột lịch đã được đánh dấu là đã xử lý (RESOLVED)");
        }
        this.status = ScheduleConflictStatus.RESOLVED;
        this.resolvedBy = userId;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void resolveWithNote(Long userId, Long handlerId, String note) {
        if (this.status == ScheduleConflictStatus.RESOLVED) {
            throw new IllegalStateException("Cảnh báo xung đột lịch đã được đánh dấu là đã xử lý (RESOLVED)");
        }
        if (note == null || note.trim().isEmpty()) {
            throw new IllegalArgumentException("Ghi chú cách xử lý xung đột không được để trống");
        }
        this.status = ScheduleConflictStatus.RESOLVED;
        this.assignedHandlerId = handlerId;
        this.resolutionNote = note.trim();
        this.resolvedBy = userId;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void assignHandler(Long handlerId) {
        this.assignedHandlerId = handlerId;
        this.updatedAt = LocalDateTime.now();
    }

    public void unassignHandler() {
        this.assignedHandlerId = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void reopenAsRecurrent(String note) {
        this.status = ScheduleConflictStatus.REOPENED;
        this.isRecurrent = true;
        this.recurrentNote = note;
        this.resolvedAt = null;
        this.resolvedBy = null;
        this.resolutionNote = null;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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
