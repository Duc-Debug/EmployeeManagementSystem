package com.hrm.employeemanagement.application.dto.report.excel;

import java.time.LocalDate;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Query DTO yêu cầu xuất báo cáo phân bổ dự án ra file Excel (NCL-10-CN-003).
 */
public record ExportReportExcelQuery(
        Long projectId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        Boolean all
) {
    public ExportReportExcelQuery {
        Objects.requireNonNull(projectId, "projectId không được null");
    }

    public ExportReportExcelQuery(Long projectId, Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek) {
        this(projectId, fromYear, fromWeek, toYear, toWeek, false);
    }

    /**
     * Chuẩn hóa và xác thực khoảng thời gian.
     * Nếu chọn all == true, giữ nguyên để Service lấy toàn bộ vòng đời dự án.
     * Nếu không truyền khoảng tuần và all != true, mặc định lấy 4 tuần an toàn theo chuẩn ISO-8601.
     */
    public ExportReportExcelQuery withDefaults() {
        if (Boolean.TRUE.equals(all)) {
            return this;
        }

        YearWeek startYw;
        if (fromYear != null && fromWeek != null) {
            startYw = YearWeek.of(fromYear, fromWeek);
        } else {
            startYw = YearWeek.from(LocalDate.now());
        }

        YearWeek endYw;
        if (toYear != null && toWeek != null) {
            endYw = YearWeek.of(toYear, toWeek);
        } else {
            // Cộng an toàn 3 tuần bằng ISO calendar thay vì cộng số học tuần
            LocalDate startMonday = startYw.getStartDate();
            endYw = YearWeek.from(startMonday.plusWeeks(3));
        }

        return new ExportReportExcelQuery(projectId, startYw.year(), startYw.weekNumber(), endYw.year(), endYw.weekNumber(), false);
    }
}
