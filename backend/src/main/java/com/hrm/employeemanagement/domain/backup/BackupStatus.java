package com.hrm.employeemanagement.domain.backup;

public enum BackupStatus {
    IN_PROGRESS("Đang xử lý"),
    COMPLETED("Hoàn tất"),
    FAILED("Thất bại");

    private final String description;

    BackupStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
