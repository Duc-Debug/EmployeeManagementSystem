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
        boolean hasFrom = fromYear != null || fromWeek != null;
        boolean hasTo = toYear != null || toWeek != null;

        if (fromYear != null && fromWeek == null) {
            throw new IllegalArgumentException("fromYear và fromWeek phải đi cùng nhau");
        }
        if (fromYear == null && fromWeek != null) {
            throw new IllegalArgumentException("fromYear và fromWeek phải đi cùng nhau");
        }
        if (toYear != null && toWeek == null) {
            throw new IllegalArgumentException("toYear và toWeek phải đi cùng nhau");
        }
        if (toYear == null && toWeek != null) {
            throw new IllegalArgumentException("toYear và toWeek phải đi cùng nhau");
        }

        if (hasFrom != hasTo) {
            throw new IllegalArgumentException("Phải cung cấp đầy đủ cả khoảng thời gian (từ năm/tuần đến năm/tuần) hoặc không cung cấp cả hai");
        }

        if (!hasFrom) {
            throw new IllegalArgumentException("Recruitment demand report requires a week range to calculate capacity accurately");
        }

        if (hasFrom && hasTo) {
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
    }

    private static int getMaxIsoWeeksInYear(int year) {
        LocalDate dec28 = LocalDate.of(year, 12, 28);
        return dec28.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
