package com.hrm.employeemanagement.application.port.outbound.notification.dedup;

public enum OverloadAlertDispatchResult {
    ALERTED,
    SKIPPED_DEDUP,
    FAILED
}
