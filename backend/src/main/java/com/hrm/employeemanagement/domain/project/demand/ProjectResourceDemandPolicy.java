package com.hrm.employeemanagement.domain.project.demand;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.exception.project.InvalidResourceDemandException;

public final class ProjectResourceDemandPolicy {

    public static final BigDecimal MAX_HOURS_PER_WEEK = new BigDecimal("168.00");
    public static final BigDecimal MIN_HOURS_PER_WEEK = new BigDecimal("0.01");

    private ProjectResourceDemandPolicy() {
        
    }

    /**
     * Phân rã khoảng thời gian từ startDate đến endDate của dự án thành danh sách các tuần (YearWeek) chuẩn ISO-8601.
     * Tuần tính từ Thứ Hai đầu tiên chứa hoặc liền trước startDate đến Chủ Nhật chứa hoặc liền sau endDate.
     */
    public static List<YearWeek> calculateProjectWeeks(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "Ngày bắt đầu dự án không được null");
        Objects.requireNonNull(endDate, "Ngày kết thúc dự án không được null");

        if (endDate.isBefore(startDate)) {
            throw InvalidProjectDateRangeException.invalidRange();
        }

        List<YearWeek> weeks = new ArrayList<>();
        // Tìm ngày Thứ Hai của tuần chứa ngày bắt đầu
        LocalDate currentMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        while (!currentMonday.isAfter(endDate)) {
            int year = currentMonday.get(WeekFields.ISO.weekBasedYear());
            int weekNumber = currentMonday.get(WeekFields.ISO.weekOfWeekBasedYear());
            weeks.add(YearWeek.of(year, weekNumber));

            currentMonday = currentMonday.plusWeeks(1);
        }

        return List.copyOf(weeks);
    }

    /**
     * Kiểm tra số giờ yêu cầu theo tuần: Bắt buộc > 0, <= 168.00, tối đa 2 chữ số thập phân.
     */
    public static void validateRequiredHours(BigDecimal hours) {
        if (hours == null) {
            throw new InvalidResourceDemandException("Số giờ nhu cầu không được để trống");
        }
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidResourceDemandException("Số giờ nhu cầu mỗi tuần phải lớn hơn 0");
        }
        if (hours.compareTo(MAX_HOURS_PER_WEEK) > 0) {
            throw new InvalidResourceDemandException("Số giờ nhu cầu mỗi tuần không được vượt quá 168 giờ");
        }
        if (hours.stripTrailingZeros().scale() > 2) {
            throw new InvalidResourceDemandException("Số giờ nhu cầu chỉ được có tối đa 2 chữ số thập phân");
        }
    }

    /**
     * Kiểm tra xem tổng nhu cầu nhân sự có vượt quá tổng giờ dự kiến của dự án hay không (TC-02).
     */
    public static boolean isExceedingBudget(BigDecimal totalDemandHours, BigDecimal projectEstimatedHours) {
        if (totalDemandHours == null || projectEstimatedHours == null) {
            return false;
        }
        if (projectEstimatedHours.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return totalDemandHours.compareTo(projectEstimatedHours) > 0;
    }
}