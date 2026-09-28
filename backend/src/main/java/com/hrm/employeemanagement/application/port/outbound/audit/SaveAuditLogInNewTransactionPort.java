package com.hrm.employeemanagement.application.port.outbound.audit;

import com.hrm.employeemanagement.domain.audit.AuditLog;

public interface SaveAuditLogInNewTransactionPort {
    AuditLog save(AuditLog auditLog);
}
