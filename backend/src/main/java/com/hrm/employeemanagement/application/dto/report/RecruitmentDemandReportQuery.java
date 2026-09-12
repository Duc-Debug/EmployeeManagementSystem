package com.hrm.employeemanagement.application.dto.report;

public record RecruitmentDemandReportQuery(
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        Long orgUnitId
) {
    public void validate() {
        if (fromWeek != null && (fromWeek < 1 || fromWeek > 53)) {
            throw new IllegalArgumentException("fromWeek phải nằm trong khoảng từ 1 đến 53");
        }
        if (toWeek != null && (toWeek < 1 || toWeek > 53)) {
            throw new IllegalArgumentException("toWeek phải nằm trong khoảng từ 1 đến 53");
        }
        if (fromYear != null && fromYear <= 0) {
            throw new IllegalArgumentException("fromYear phải là số nguyên dương");
        }
        if (toYear != null && toYear <= 0) {
            throw new IllegalArgumentException("toYear phải là số nguyên dương");
        }
        if (fromYear != null && toYear != null) {
            if (fromYear > toYear) {
                throw new IllegalArgumentException("Thời gian bắt đầu (fromYear) không được lớn hơn thời gian kết thúc (toYear)");
            }
            if (fromYear.equals(toYear) && fromWeek != null && toWeek != null && fromWeek > toWeek) {
                throw new IllegalArgumentException("Tuần bắt đầu (fromWeek) không được lớn hơn tuần kết thúc (toWeek) trong cùng một năm");
            }
        }
    }
}
