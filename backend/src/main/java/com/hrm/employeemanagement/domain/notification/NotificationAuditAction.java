package com.hrm.employeemanagement.domain.notification;

/**
 * 3 hành động state-changing của Thành viên được phép ghi vào notification_audit_log (TC-03).
 */
public enum NotificationAuditAction {
    MARK_READ,
    MARK_ALL_READ,
    DELETE_NOTIFICATION
}
