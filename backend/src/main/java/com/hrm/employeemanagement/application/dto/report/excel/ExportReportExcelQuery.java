package com.hrm.employeemanagement.application.dto.report.excel;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Objects;

/**
 * Query DTO yêu cầu xuất báo cáo phân bổ dự án ra file Excel (NCL-10-CN-003).
 */
public record ExportReportExcelQuery(
        Long projectId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek
) {
    public ExportReportExcelQuery {
        Objects.requireNonNull(projectId, "projectId không được null");
    }

    /**
     * Chuẩn hóa và xác thực khoảng thời gian.
     * Nếu không truyền khoảng tuần, mặc định lấy 4 tuần từ tuần hiện tại.
     */
    public ExportReportExcelQuery withDefaults() {
        int fYear = fromYear != null ? fromYear : LocalDate.now().get(IsoFields.WEEK_BASED_YEAR);
        int fWeek = fromWeek != null ? fromWeek : LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int tYear = toYear != null ? toYear : fYear;
        int tWeek = toWeek != null ? toWeek : (fWeek + 3);

        return new ExportReportExcelQuery(projectId, fYear, fWeek, tYear, tWeek);
    }
}
