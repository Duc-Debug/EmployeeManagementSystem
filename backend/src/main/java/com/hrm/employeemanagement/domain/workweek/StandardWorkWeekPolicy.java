package com.hrm.employeemanagement.domain.workweek;

import com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StandardWorkWeekPolicy {

    public static final BigDecimal MIN_HOURS_PER_WEEK = BigDecimal.valueOf(4.00);
    public static final BigDecimal MAX_HOURS_PER_WEEK = BigDecimal.valueOf(84.00);

    public static void validateDays(List<StandardWorkWeekDay> days) {
        if (days == null || days.size() != 7) {
            throw new InvalidStandardWorkWeekException("Cấu hình tuần làm việc phải có đầy đủ 7 ngày trong tuần");
        }

        Map<DayOfWeek, StandardWorkWeekDay> dayMap = new EnumMap<>(DayOfWeek.class);
        boolean hasWorkingDay = false;

        for (StandardWorkWeekDay day : days) {
            if (day == null || day.getDayOfWeek() == null) {
                throw new InvalidStandardWorkWeekException("Thông tin ngày trong tuần không được null");
            }
            if (dayMap.put(day.getDayOfWeek(), day) != null) {
                throw new InvalidStandardWorkWeekException("Trùng lặp cấu hình cho ngày: " + day.getDayOfWeek());
            }
            if (day.isWorkingDay()) {
                hasWorkingDay = true;
            }
        }

        if (dayMap.size() != 7) {
            throw new InvalidStandardWorkWeekException("Cấu hình tuần làm việc phải có đầy đủ 7 ngày trong tuần");
        }

        if (!hasWorkingDay) {
            throw new InvalidStandardWorkWeekException("Tuần làm việc phải có ít nhất một ngày làm việc hoạt động");
        }

        BigDecimal totalHours = calculateStandardHoursPerWeek(days);
        if (totalHours.compareTo(MIN_HOURS_PER_WEEK) < 0 || totalHours.compareTo(MAX_HOURS_PER_WEEK) > 0) {
            throw new InvalidStandardWorkWeekException(
                    "Tổng số giờ làm việc chuẩn mỗi tuần phải từ " + MIN_HOURS_PER_WEEK + " đến " + MAX_HOURS_PER_WEEK + " giờ");
        }
    }

    public static BigDecimal calculateStandardHoursPerWeek(List<StandardWorkWeekDay> days) {
        if (days == null) return BigDecimal.ZERO;
        return days.stream()
                .filter(StandardWorkWeekDay::isWorkingDay)
                .map(StandardWorkWeekDay::getWorkingHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal convertCapacity(
            BigDecimal value,
            CapacityUnit from,
            CapacityUnit to,
            BigDecimal hoursPerDay,
            BigDecimal hoursPerWeek) {

        Objects.requireNonNull(value, "Giá trị cần quy đổi không được null");
        Objects.requireNonNull(from, "Đơn vị nguồn không được null");
        Objects.requireNonNull(to, "Đơn vị đích không được null");
        Objects.requireNonNull(hoursPerDay, "Giờ làm việc mỗi ngày không được null");
        Objects.requireNonNull(hoursPerWeek, "Giờ làm việc mỗi tuần không được null");

        if (hoursPerDay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidStandardWorkWeekException("Giờ làm việc chuẩn mỗi ngày phải lớn hơn 0 để quy đổi");
        }
        if (hoursPerWeek.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidStandardWorkWeekException("Giờ làm việc chuẩn mỗi tuần phải lớn hơn 0 để quy đổi");
        }

        if (from == to) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }

        // Chuyển sang đơn vị cơ sở: HOURS
        BigDecimal hours;
        switch (from) {
            case DAYS -> hours = value.multiply(hoursPerDay);
            case FTE -> hours = value.multiply(hoursPerWeek);
            case HOURS -> hours = value;
            default -> throw new IllegalArgumentException("Unsupported unit: " + from);
        }

        // Chuyển từ HOURS sang đơn vị đích:
        return switch (to) {
            case HOURS -> hours.setScale(2, RoundingMode.HALF_UP);
            case DAYS -> hours.divide(hoursPerDay, 2, RoundingMode.HALF_UP);
            case FTE -> hours.divide(hoursPerWeek, 2, RoundingMode.HALF_UP);
        };
    }
}

