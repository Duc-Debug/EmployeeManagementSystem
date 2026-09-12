package com.hrm.employeemanagement.application.dto.report;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

public record RecruitmentDemandReportQuery(
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        Long orgUnitId
) {
    public void validate() {
        if (fromYear == null || fromWeek == null || toYear == null || toWeek == null) {
            throw new IllegalArgumentException("Báo cáo nhu cầu tuyển dụng bắt buộc phải cung cấp đầy đủ khoảng thời gian (fromYear, fromWeek, toYear, toWeek)");
        }

        if (fromYear <= 0) {
            throw new IllegalArgumentException("fromYear phải là số nguyên dương");
        }
        if (toYear <= 0) {
            throw new IllegalArgumentException("toYear phải là số nguyên dương");
        }

        int maxFromWeeks = getMaxIsoWeeksInYear(fromYear);
        if (fromWeek < 1 || fromWeek > maxFromWeeks) {
            throw new IllegalArgumentException("Tuần bắt đầu (fromWeek) " + fromWeek + " không hợp lệ cho năm " + fromYear + " (tối đa " + maxFromWeeks + " tuần)");
        }

        int maxToWeeks = getMaxIsoWeeksInYear(toYear);
        if (toWeek < 1 || toWeek > maxToWeeks) {
            throw new IllegalArgumentException("Tuần kết thúc (toWeek) " + toWeek + " không hợp lệ cho năm " + toYear + " (tối đa " + maxToWeeks + " tuần)");
        }

        if (fromYear > toYear || (fromYear.equals(toYear) && fromWeek > toWeek)) {
            throw new IllegalArgumentException("Thời gian bắt đầu (fromYear/fromWeek) không được lớn hơn thời gian kết thúc (toYear/toWeek)");
        }
    }

    private static int getMaxIsoWeeksInYear(int year) {
        LocalDate dec28 = LocalDate.of(year, 12, 28);
        return dec28.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
