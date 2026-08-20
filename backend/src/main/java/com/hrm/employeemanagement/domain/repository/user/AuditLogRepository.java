package com.hrm.employeemanagement.domain.repository.user;

import com.hrm.employeemanagement.domain.model.audit.AuditLog;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
}
