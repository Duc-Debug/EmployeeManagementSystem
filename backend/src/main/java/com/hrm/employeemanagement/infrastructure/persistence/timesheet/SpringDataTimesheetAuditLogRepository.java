package com.hrm.employeemanagement.infrastructure.persistence.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTimesheetAuditLogRepository extends JpaRepository<TimesheetAuditLogJpaEntity, Long> {
}
