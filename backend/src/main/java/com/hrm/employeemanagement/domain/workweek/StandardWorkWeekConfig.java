package com.hrm.employeemanagement.domain.workweek;

import com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StandardWorkWeekConfig {

    private Long id;
    private final WorkWeekScope scope;
    private CapacityUnit capacityUnit;
    private WeekStartDay weekStartDay;
    private BigDecimal standardHoursPerDay;
    private BigDecimal standardHoursPerWeek;
    private List<StandardWorkWeekDay> days;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public StandardWorkWeekConfig(
            Long id,
            WorkWeekScope scope,
            CapacityUnit capacityUnit,
            WeekStartDay weekStartDay,
            BigDecimal standardHoursPerDay,
            List<StandardWorkWeekDay> days,
            Long createdBy,
            Long updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {

        this.id = id;
        this.scope = Objects.requireNonNull(scope, "WorkWeekScope không được null");
        this.capacityUnit = capacityUnit != null ? capacityUnit : CapacityUnit.HOURS;
        this.weekStartDay = weekStartDay != null ? weekStartDay : WeekStartDay.MONDAY;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;

        setDaysAndHours(days, standardHoursPerDay);
    }

    public static StandardWorkWeekConfig createDefaultCompany(Long createdBy) {
        List<StandardWorkWeekDay> defaultDays = List.of(
                StandardWorkWeekDay.working(DayOfWeek.MONDAY, BigDecimal.valueOf(8.00)),
                StandardWorkWeekDay.working(DayOfWeek.TUESDAY, BigDecimal.valueOf(8.00)),
                StandardWorkWeekDay.working(DayOfWeek.WEDNESDAY, BigDecimal.valueOf(8.00)),
                StandardWorkWeekDay.working(DayOfWeek.THURSDAY, BigDecimal.valueOf(8.00)),
                StandardWorkWeekDay.working(DayOfWeek.FRIDAY, BigDecimal.valueOf(8.00)),
                StandardWorkWeekDay.nonWorking(DayOfWeek.SATURDAY),
                StandardWorkWeekDay.nonWorking(DayOfWeek.SUNDAY)
        );

        return new StandardWorkWeekConfig(
                null,
                WorkWeekScope.company(),
                CapacityUnit.HOURS,
                WeekStartDay.MONDAY,
                BigDecimal.valueOf(8.00),
                defaultDays,
                createdBy,
                null,
                LocalDateTime.now(),
                null,
                0L
        );
    }

    public void update(
            CapacityUnit capacityUnit,
            WeekStartDay weekStartDay,
            BigDecimal standardHoursPerDay,
            List<StandardWorkWeekDay> days,
            Long updatedBy) {

        if (capacityUnit != null) {
            this.capacityUnit = capacityUnit;
        }
        if (weekStartDay != null) {
            this.weekStartDay = weekStartDay;
        }
        setDaysAndHours(days, standardHoursPerDay);
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    private void setDaysAndHours(List<StandardWorkWeekDay> newDays, BigDecimal newHoursPerDay) {
        StandardWorkWeekPolicy.validateDays(newDays);

        BigDecimal hoursPerDay = newHoursPerDay != null
                ? newHoursPerDay.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(8.00);

        if (hoursPerDay.compareTo(StandardWorkWeekDay.MIN_WORKING_HOURS) < 0
                || hoursPerDay.compareTo(StandardWorkWeekDay.MAX_WORKING_HOURS) > 0) {
            throw new InvalidStandardWorkWeekException(
                    "Số giờ làm việc chuẩn mỗi ngày phải từ " + StandardWorkWeekDay.MIN_WORKING_HOURS + " đến " + StandardWorkWeekDay.MAX_WORKING_HOURS + " giờ");
        }

        this.standardHoursPerDay = hoursPerDay;
        this.days = List.copyOf(newDays);
        this.standardHoursPerWeek = StandardWorkWeekPolicy.calculateStandardHoursPerWeek(newDays);
    }

    public BigDecimal toDays(BigDecimal hours) {
        return StandardWorkWeekPolicy.convertCapacity(hours, CapacityUnit.HOURS, CapacityUnit.DAYS, standardHoursPerDay, standardHoursPerWeek);
    }

    public BigDecimal toFte(BigDecimal hours) {
        return StandardWorkWeekPolicy.convertCapacity(hours, CapacityUnit.HOURS, CapacityUnit.FTE, standardHoursPerDay, standardHoursPerWeek);
    }

    public BigDecimal toHours(BigDecimal value, CapacityUnit unit) {
        return StandardWorkWeekPolicy.convertCapacity(value, unit, CapacityUnit.HOURS, standardHoursPerDay, standardHoursPerWeek);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkWeekScope getScope() {
        return scope;
    }

    public CapacityUnit getCapacityUnit() {
        return capacityUnit;
    }

    public WeekStartDay getWeekStartDay() {
        return weekStartDay;
    }

    public BigDecimal getStandardHoursPerDay() {
        return standardHoursPerDay;
    }

    public BigDecimal getStandardHoursPerWeek() {
        return standardHoursPerWeek;
    }

    public List<StandardWorkWeekDay> getDays() {
        return Collections.unmodifiableList(days);
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}

