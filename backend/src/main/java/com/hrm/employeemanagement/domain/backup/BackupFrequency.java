package com.hrm.employeemanagement.domain.backup;

public enum BackupFrequency {
    DAILY("Hàng ngày"),
    WEEKLY("Hàng tuần");

    private final String label;

    BackupFrequency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BackupFrequency fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DAILY;
        }
        for (BackupFrequency freq : values()) {
            if (freq.name().equalsIgnoreCase(value.trim())) {
                return freq;
            }
        }
        return DAILY;
    }
}
