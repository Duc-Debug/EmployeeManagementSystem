package com.hrm.employeemanagement.domain.backup.exception;

public class BackupNotFoundException extends RuntimeException {
    public BackupNotFoundException(Long id) {
        super("Không tìm thấy bản sao lưu với ID: " + id);
    }

    public BackupNotFoundException(String message) {
        super(message);
    }
}
