package com.hrm.employeemanagement.domain.backup;

public enum BackupAction {
    BACKUP_CREATE("Tạo bản sao lưu"),
    BACKUP_RESTORE("Phục hồi dữ liệu"),
    BACKUP_DELETE("Xóa bản sao lưu"),
    BACKUP_DOWNLOAD("Tải về bản sao lưu"),
    BACKUP_UPLOAD("Tải lên bản sao lưu"),
    SCHEDULE_UPDATE("Cập nhật lịch sao lưu"),
    ACCESS_DENIED("Từ chối truy cập trái phép");

    private final String description;

    BackupAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
