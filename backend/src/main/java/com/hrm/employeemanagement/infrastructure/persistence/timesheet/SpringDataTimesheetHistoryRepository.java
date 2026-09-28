package com.hrm.employeemanagement.infrastructure.persistence.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTimesheetHistoryRepository extends JpaRepository<TimesheetHistoryJpaEntity, Long> {
}
