package com.hrm.employeemanagement.application.dto.projecttemplate;

import java.time.LocalDate;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
/**
 * Dữ liệu đầu vào để tạo dự án từ mẫu
 */
public record CreateProjectFromTemplateCommand(
        Long templateId,
        String projectName,
        Long orgUnitId,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate,
        String description) {
    public CreateProjectFromTemplateCommand {
        Objects.requireNonNull(templateId, "Mã mẫu dự án (templateId) không được để trống");
        if (projectName == null || projectName.isBlank()) {
            throw new InvalidProjectDataException("Tên dự án không được để trống");
        }
        Objects.requireNonNull(orgUnitId, "Đơn vị tổ chức (orgUnitId) không được để trống");
    }
}
