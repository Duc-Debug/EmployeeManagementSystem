package com.hrm.employeemanagement.application.dto.project;

/**
 * DTO mang du lieu yeu cau huy du an du kien tu Controller vao Service.
 */
public record CancelProjectCommand(
        Long projectId,
        String cancelReason) {
}
