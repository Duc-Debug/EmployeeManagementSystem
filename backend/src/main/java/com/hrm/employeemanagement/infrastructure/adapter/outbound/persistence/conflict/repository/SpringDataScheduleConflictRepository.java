package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictJpaEntity;

public interface SpringDataScheduleConflictRepository extends JpaRepository<ScheduleConflictJpaEntity, Long> {

    @Query("""
        SELECT c FROM ScheduleConflictJpaEntity c
        WHERE (:yearNumber IS NULL OR c.yearNumber = :yearNumber)
          AND (:startWeek IS NULL OR c.weekNumber >= :startWeek)
          AND (:endWeek IS NULL OR c.weekNumber <= :endWeek)
          AND (:employeeId IS NULL OR c.employeeId = :employeeId)
          AND (:conflictType IS NULL OR c.conflictType = :conflictType)
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.yearNumber DESC, c.weekNumber DESC, c.id DESC
    """)
    List<ScheduleConflictJpaEntity> findConflicts(
            @Param("yearNumber") Integer yearNumber,
            @Param("startWeek") Integer startWeek,
            @Param("endWeek") Integer endWeek,
            @Param("employeeId") Long employeeId,
            @Param("conflictType") ConflictType conflictType,
            @Param("status") ScheduleConflictStatus status
    );

    Optional<ScheduleConflictJpaEntity> findFirstByEmployeeIdAndYearNumberAndWeekNumberAndConflictType(
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            ConflictType conflictType
    );
}
