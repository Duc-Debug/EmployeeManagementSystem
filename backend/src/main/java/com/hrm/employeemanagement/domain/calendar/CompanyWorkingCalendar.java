package com.hrm.employeemanagement.domain.calendar;

import com.hrm.employeemanagement.domain.exception.calendar.InvalidWorkingCalendarException;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain Entity đại diện cho Lịch làm việc tuần tiêu chuẩn của công ty.
 */
public class CompanyWorkingCalendar {

    private final Map<DayOfWeek, Boolean> schedule;

    public CompanyWorkingCalendar(List<WorkingCalendarDay> days) {
        Objects.requireNonNull(days, "Danh sách ngày làm việc không được null");
        if (days.isEmpty()) {
            throw new InvalidWorkingCalendarException("Danh sách ngày làm việc trong tuần không được để trống");
        }

        this.schedule = new EnumMap<>(DayOfWeek.class);
        for (WorkingCalendarDay day : days) {
            if (day != null && day.dayOfWeek() != null) {
                this.schedule.put(day.dayOfWeek(), day.isWorkingDay());
            }
        }

        validateInvariant();
    }

    private void validateInvariant() {
        boolean hasAtLeastOneWorkingDay = schedule.values().stream().anyMatch(Boolean::booleanValue);
        if (!hasAtLeastOneWorkingDay) {
            throw new InvalidWorkingCalendarException("Tuần làm việc phải có ít nhất một ngày làm việc hoạt động");
        }
    }

    public static CompanyWorkingCalendar createDefault() {
        return new CompanyWorkingCalendar(List.of(
                new WorkingCalendarDay(DayOfWeek.MONDAY, true),
                new WorkingCalendarDay(DayOfWeek.TUESDAY, true),
                new WorkingCalendarDay(DayOfWeek.WEDNESDAY, true),
                new WorkingCalendarDay(DayOfWeek.THURSDAY, true),
                new WorkingCalendarDay(DayOfWeek.FRIDAY, true),
                new WorkingCalendarDay(DayOfWeek.SATURDAY, false),
                new WorkingCalendarDay(DayOfWeek.SUNDAY, false)
        ));
    }

    public boolean isWorkingDay(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return false;
        }
        return schedule.getOrDefault(dayOfWeek, false);
    }

    public Set<DayOfWeek> getWorkingDays() {
        return schedule.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public List<WorkingCalendarDay> getDays() {
        return List.of(DayOfWeek.values()).stream()
                .map(dow -> new WorkingCalendarDay(dow, isWorkingDay(dow)))
                .toList();
    }

    public Map<DayOfWeek, Boolean> getSchedule() {
        return Collections.unmodifiableMap(schedule);
    }
}
