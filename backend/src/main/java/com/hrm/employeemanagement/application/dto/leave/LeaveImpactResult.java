package com.hrm.employeemanagement.application.dto.leave;

import java.math.BigDecimal;
import java.util.List;

/**
 * TC-02: DTO chứa thông tin các dự án và số giờ phân bổ bị ảnh hưởng trong tuần nghỉ.
 */
public record LeaveImpactResult(
        Long leaveRequestId,
        Long employeeId,
        String employeeName,
        String startDate,
        String endDate,
        int daysCount,
        BigDecimal hoursDeducted,
        BigDecimal totalAllocatedHoursInLeavePeriod,
        boolean hasConflict,
        List<ProjectAllocationImpact> affectedProjects
) {
    public record ProjectAllocationImpact(
            Long projectId,
            String projectName,
            Integer year,
            Integer weekNumber,
            BigDecimal allocatedHours
    ) {}
}
