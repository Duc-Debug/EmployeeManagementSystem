package com.hrm.employeemanagement.application.dto.project;
/**
 * DTO chứa thông tin tóm tắt các task chưa hoàn thành (để trả về khi chặn đóng dự án).
 */
public record UnfinishedTaskSummary(
        Long taskId,
        String taskCode,
        String taskName,
        String status,
        Long assigneeId) {
}
