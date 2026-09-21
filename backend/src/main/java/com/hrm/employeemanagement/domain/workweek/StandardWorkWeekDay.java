package com.hrm.employeemanagement.domain.workweek;

import com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Objects;

public class StandardWorkWeekDay {

    public static final BigDecimal MIN_WORKING_HOURS = BigDecimal.valueOf(0.5);
    public static final BigDecimal MAX_WORKING_HOURS = BigDecimal.valueOf(12.0);

    private final DayOfWeek dayOfWeek;
    private final boolean isWorkingDay;
    private final BigDecimal workingHours;

    public StandardWorkWeekDay(DayOfWeek dayOfWeek, boolean isWorkingDay, BigDecimal workingHours) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "Thứ trong tuần không được null");
        this.isWorkingDay = isWorkingDay;

        if (!isWorkingDay) {
            this.workingHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal hours = workingHours != null ? workingHours.setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(8.00);
            if (hours.compareTo(MIN_WORKING_HOURS) < 0 || hours.compareTo(MAX_WORKING_HOURS) > 0) {
                throw new InvalidStandardWorkWeekException(
                        "Số giờ làm việc của ngày " + dayOfWeek + " phải từ " + MIN_WORKING_HOURS + " đến " + MAX_WORKING_HOURS + " giờ");
            }
            this.workingHours = hours;
        }
    }

    public static StandardWorkWeekDay working(DayOfWeek dayOfWeek, BigDecimal hours) {
        return new StandardWorkWeekDay(dayOfWeek, true, hours);
    }

    public static StandardWorkWeekDay nonWorking(DayOfWeek dayOfWeek) {
        return new StandardWorkWeekDay(dayOfWeek, false, BigDecimal.ZERO);
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean isWorkingDay() {
        return isWorkingDay;
    }

    public BigDecimal getWorkingHours() {
        return workingHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StandardWorkWeekDay that)) return false;
        return isWorkingDay == that.isWorkingDay &&
                dayOfWeek == that.dayOfWeek &&
                Objects.equals(workingHours, that.workingHours);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, isWorkingDay, workingHours);
    }
}

