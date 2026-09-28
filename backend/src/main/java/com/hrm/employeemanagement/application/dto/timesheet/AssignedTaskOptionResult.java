package com.hrm.employeemanagement.application.dto.timesheet;
/**
 * Trả về danh sách các công việc (tasks) thuộc dự án đang chạy (ACTIVE) mà nhân viên hiện tại được phân công.
 */
public record AssignedTaskOptionResult(
                Long projectId,
                String projectCode,
                String projectName,
                String projectStatus,
                Long taskId,
                String taskCode,
                String taskName,
                String taskStatus) {
}