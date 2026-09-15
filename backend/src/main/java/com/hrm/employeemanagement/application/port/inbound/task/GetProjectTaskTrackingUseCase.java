package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.tracking.ProjectTaskTrackingResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingQuery;

/**
 * Inbound port cho Use Case xem bảng theo dõi công việc dự án (NCL-04-CN-003).
 */
public interface GetProjectTaskTrackingUseCase {

    ProjectTaskTrackingResult getTaskTracking(TaskTrackingQuery query);
}
