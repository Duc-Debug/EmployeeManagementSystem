package com.hrm.employeemanagement.application.dto.task.tracking;

import com.hrm.employeemanagement.domain.task.TaskStatus;

/**
 * Query tham số tra cứu bảng theo dõi công việc dự án (NCL-04-CN-003).
 */
public record TaskTrackingQuery(
        Long projectId,
        Long employeeId,
        TaskStatus status,
        Boolean overdueOnly
) {
}
