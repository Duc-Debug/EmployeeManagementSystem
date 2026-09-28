package com.hrm.employeemanagement.domain.workweek;

public enum CapacityUnit {
    HOURS("Giờ"),
    DAYS("Ngày công"),
    FTE("FTE (Full-Time Equivalent)");

    private final String displayName;

    CapacityUnit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

