package com.hrm.employeemanagement.domain.workweek;

import java.time.DayOfWeek;

public enum WeekStartDay {
    MONDAY(DayOfWeek.MONDAY, "Thứ Hai"),
    SUNDAY(DayOfWeek.SUNDAY, "Chủ Nhật");

    private final DayOfWeek dayOfWeek;
    private final String displayName;

    WeekStartDay(DayOfWeek dayOfWeek, String displayName) {
        this.dayOfWeek = dayOfWeek;
        this.displayName = displayName;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public String getDisplayName() {
        return displayName;
    }
}

