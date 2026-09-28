package com.hrm.employeemanagement.application.dto.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * Trả về thông tin đầy đủ của một dòng giờ công sau khi tạo hoặc cập nhật.
 */
public record WorkLogResult(
        Long id,
        Long timesheetId,
        Long employeeId,
        String employeeName,
        Long projectId,
        String projectCode,
        String projectName,
        Long taskId,
        String taskCode,
        String taskName,
        LocalDate workDate,
        BigDecimal hours,
        boolean isBillable,
        String description,
        String status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version,
        BigDecimal taskBudgetHours,
        BigDecimal taskActualHours,
        BigDecimal taskPendingHours
) {
    public WorkLogResult(Long id, Long timesheetId, Long employeeId, String employeeName,
            Long projectId, String projectCode, String projectName, Long taskId, String taskCode,
            String taskName, LocalDate workDate, BigDecimal hours, boolean isBillable,
            String description, String status, String rejectionReason, LocalDateTime createdAt,
            LocalDateTime updatedAt, Long version) {
        this(id, timesheetId, employeeId, employeeName, projectId, projectCode, projectName,
                taskId, taskCode, taskName, workDate, hours, isBillable, description, status,
                rejectionReason, createdAt, updatedAt, version, null, null, null);
    }
}
