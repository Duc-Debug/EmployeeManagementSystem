package com.hrm.employeemanagement.application.port.outbound.leave;

public interface SaveLeaveAuditLogPort {
    void recordAudit(Long userId, String action, String description);
}
