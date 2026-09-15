package com.hrm.employeemanagement.infrastructure.persistence.timesheet;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTimesheetRepository extends JpaRepository<TimesheetJpaEntity, Long> {
    Optional<TimesheetJpaEntity> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);

    java.util.List<TimesheetJpaEntity> findByWeekStartDateLessThanEqualAndStatusAndRemindedAtIsNull(LocalDate date, String status, org.springframework.data.domain.Pageable pageable);
}
