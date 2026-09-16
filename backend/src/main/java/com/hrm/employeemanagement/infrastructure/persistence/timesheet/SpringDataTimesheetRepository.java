package com.hrm.employeemanagement.infrastructure.persistence.timesheet;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTimesheetRepository extends JpaRepository<TimesheetJpaEntity, Long> {
    Optional<TimesheetJpaEntity> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);

    @Query("""
            SELECT t FROM TimesheetJpaEntity t
            WHERE t.weekStartDate <= :date
              AND t.status = :status
              AND (t.reminderStatus = 'PENDING'
                   OR (t.reminderStatus = 'RETRY_PENDING'
                       AND (t.nextReminderAt IS NULL OR t.nextReminderAt <= :now)))
            ORDER BY t.weekStartDate ASC, t.id ASC
            """)
    java.util.List<TimesheetJpaEntity> findReminderCandidates(
            @Param("date") LocalDate date,
            @Param("status") String status,
            @Param("now") java.time.LocalDateTime now,
            org.springframework.data.domain.Pageable pageable);
}
