package com.hrm.employeemanagement.application.dto.conflict;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;

public record ScheduleConflictResult(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String departmentName,
        Integer yearNumber,
        Integer weekNumber,
        String weekLabel,
        ConflictType conflictType,
        String conflictTypeLabel,
        String projectIds,
        String projectNames,
        Long leaveRequestId,
        String leaveInfo,
        BigDecimal totalAllocatedHours,
        BigDecimal netAvailableHours,
        BigDecimal excessHours,
        ScheduleConflictStatus status,
        String statusLabel,
        String details,
        LocalDateTime notifiedAt,
        Long notifiedBy,
        String notifiedByName,
        Long assignedHandlerId,
        String assignedHandlerCode,
        String assignedHandlerName,
        String resolutionNote,
        Boolean isRecurrent,
        String recurrentNote,
        LocalDateTime resolvedAt,
        Long resolvedBy,
        String resolvedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
