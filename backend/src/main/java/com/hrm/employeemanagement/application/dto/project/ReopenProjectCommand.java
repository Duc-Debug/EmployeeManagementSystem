package com.hrm.employeemanagement.application.dto.project;
/**
 * DTO mang dữ liệu yêu cầu mở lại dự án từ Controller vào Service.
 */
public record ReopenProjectCommand(
        Long projectId,
        String reopenReason) {
}
