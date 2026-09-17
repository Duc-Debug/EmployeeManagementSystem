package com.hrm.employeemanagement.domain.report.excel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Value Object chứa thông tin tiêu đề và ngữ cảnh của báo cáo xuất Excel.
 */
public record ExcelReportMetadata(
        String reportTitle,
        Long projectId,
        String projectCode,
        String projectName,
        String projectManagerName,
        String exportedByUsername,
        String timeRangeText,
        List<YearWeek> targetWeeks,
        LocalDateTime generatedAt
) {
    public ExcelReportMetadata {
        Objects.requireNonNull(reportTitle, "reportTitle không được null");
        Objects.requireNonNull(projectCode, "projectCode không được null");
        Objects.requireNonNull(projectName, "projectName không được null");
        Objects.requireNonNull(exportedByUsername, "exportedByUsername không được null");
        Objects.requireNonNull(targetWeeks, "targetWeeks không được null");
        if (targetWeeks.isEmpty()) {
            throw new IllegalArgumentException("targetWeeks không được rỗng");
        }
        generatedAt = generatedAt != null ? generatedAt : LocalDateTime.now();
    }
}
