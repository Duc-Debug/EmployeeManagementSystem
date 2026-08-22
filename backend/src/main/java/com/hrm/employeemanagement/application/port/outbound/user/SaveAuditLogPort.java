package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.audit.AuditLog;

public interface SaveAuditLogPort {
    AuditLog save(AuditLog auditLog);
}
