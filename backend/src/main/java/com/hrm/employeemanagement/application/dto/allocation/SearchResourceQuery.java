package com.hrm.employeemanagement.application.dto.allocation;

import java.util.Objects;

/**
 * NCL-02-CN-004: Query tìm kiếm nhân sự theo kỹ năng và độ rảnh trong khoảng
 * tuần.
 */
public record SearchResourceQuery(
        Long skillId,
        Integer minProficiencyLevel, // 1 -> 5 (mặc định nếu null là 1)
        Long orgUnitId, // Tùy chọn: lọc theo phòng ban/bộ phận cụ thể
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek
        ) {

    public SearchResourceQuery {
        Objects.requireNonNull(skillId, "skillId không được để trống");
        Objects.requireNonNull(fromYear, "fromYear không được để trống");
        Objects.requireNonNull(fromWeek, "fromWeek không được để trống");
        Objects.requireNonNull(toYear, "toYear không được để trống");
        Objects.requireNonNull(toWeek, "toWeek không được để trống");

        if (minProficiencyLevel == null) {
            minProficiencyLevel = 1;
        }
        if (minProficiencyLevel < 1 || minProficiencyLevel > 5) {
            throw new IllegalArgumentException("Mức độ thành thạo tối thiểu phải từ 1 đến 5");
        }
        if (fromWeek < 1 || fromWeek > 53 || toWeek < 1 || toWeek > 53) {
            throw new IllegalArgumentException("Số tuần phải nằm trong khoảng từ 1 đến 53");
        }
    }
}
