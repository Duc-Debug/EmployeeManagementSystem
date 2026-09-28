package com.hrm.employeemanagement.domain.backup;

public enum BackupType {
    FULL("Toàn bộ hệ thống"),
    RESOURCE_PLAN("Kế hoạch nguồn lực & Chấm công");

    private final String label;

    BackupType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BackupType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FULL;
        }
        for (BackupType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return FULL;
    }
}
