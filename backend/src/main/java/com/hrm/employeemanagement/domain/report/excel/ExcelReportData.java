package com.hrm.employeemanagement.domain.report.excel;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Đối tượng dữ liệu tổng thể chứa metadata và các dòng dữ liệu để tạo bảng tính Excel.
 */
public class ExcelReportData {

    private final ExcelReportMetadata metadata;
    private final List<ProjectAllocationExcelRow> rows;
    private final BigDecimal totalAllocatedHours;

    public ExcelReportData(
            ExcelReportMetadata metadata,
            List<ProjectAllocationExcelRow> rows,
            BigDecimal totalAllocatedHours
    ) {
        this.metadata = Objects.requireNonNull(metadata, "metadata không được null");
        this.rows = rows != null ? List.copyOf(rows) : Collections.emptyList();
        this.totalAllocatedHours = totalAllocatedHours != null ? totalAllocatedHours : BigDecimal.ZERO;
    }

    public ExcelReportMetadata getMetadata() {
        return metadata;
    }

    public List<ProjectAllocationExcelRow> getRows() {
        return rows;
    }

    public BigDecimal getTotalAllocatedHours() {
        return totalAllocatedHours;
    }

    public boolean isEmpty() {
        return rows.isEmpty() || totalAllocatedHours.compareTo(BigDecimal.ZERO) == 0;
    }
}
