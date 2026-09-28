package com.hrm.employeemanagement.application.dto.allocation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * NCL-02-CN-004: Query tìm kiếm nhân sự theo kỹ năng và độ rảnh trong khoảng tuần.
 *
 * @param skillId ID kỹ năng cần tìm kiếm (bắt buộc)
 * @param minProficiencyLevel Mức độ thành thạo tối thiểu từ 1 đến 5 (mặc định 1)
 * @param orgUnitId Tùy chọn lọc theo đơn vị/bộ phận cụ thể
 * @param fromYear Năm bắt đầu
 * @param fromWeek Tuần bắt đầu theo chuẩn ISO-8601
 * @param toYear Năm kết thúc
 * @param toWeek Tuần kết thúc theo chuẩn ISO-8601
 * @param page Số trang cần lấy (0-indexed, tùy chọn)
 * @param size Kích thước trang (tùy chọn)
 */
public record SearchResourceQuery(
        Long skillId,
        Integer minProficiencyLevel,
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        Integer page,
        Integer size
) {

    public SearchResourceQuery(
            Long skillId,
            Integer minProficiencyLevel,
            Long orgUnitId,
            Integer fromYear,
            Integer fromWeek,
            Integer toYear,
            Integer toWeek
    ) {
        this(skillId, minProficiencyLevel, orgUnitId, fromYear, fromWeek, toYear, toWeek, null, null);
    }

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

        // Kiểm tra số tuần tối đa hợp lệ theo chuẩn ISO-8601 của năm
        int maxFromWeeks = LocalDate.of(fromYear, 12, 28).get(WeekFields.ISO.weekOfWeekBasedYear());
        if (fromWeek > maxFromWeeks) {
            throw new IllegalArgumentException("Năm " + fromYear + " chỉ có " + maxFromWeeks + " tuần");
        }
        int maxToWeeks = LocalDate.of(toYear, 12, 28).get(WeekFields.ISO.weekOfWeekBasedYear());
        if (toWeek > maxToWeeks) {
            throw new IllegalArgumentException("Năm " + toYear + " chỉ có " + maxToWeeks + " tuần");
        }

        YearWeek from = YearWeek.of(fromYear, fromWeek);
        YearWeek to = YearWeek.of(toYear, toWeek);

        if (from.getStartDate().isAfter(to.getStartDate())) {
            throw new IllegalArgumentException(
                    "Khoảng thời gian bắt đầu phải nhỏ hơn hoặc bằng khoảng thời gian kết thúc"
            );
        }

        long weekCount = ChronoUnit.WEEKS.between(from.getStartDate(), to.getStartDate()) + 1;
        if (weekCount > 52) {
            throw new IllegalArgumentException("Khoảng thời gian tìm kiếm không được vượt quá 52 tuần");
        }

        if (page != null && page < 0) {
            throw new IllegalArgumentException("Số trang (page) không được nhỏ hơn 0");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Kích thước trang (size) phải lớn hơn 0");
        }
        if (size != null && size > 100) {
            throw new IllegalArgumentException("Kích thước trang không được vượt quá 100");
        }
    }
}
