package com.hrm.employeemanagement.application.dto.project;
/**
 * DTO mang dữ liệu yêu cầu đóng dự án từ Controller vào Service.
 */
public record CloseProjectCommand(
        Long projectId,
        String closureReason) {
}
