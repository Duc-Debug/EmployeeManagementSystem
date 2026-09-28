package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanningPeriodJpaEntity;

public interface SpringDataAllocationPlanningPeriodRepository extends JpaRepository<AllocationPlanningPeriodJpaEntity, Long> {

    List<AllocationPlanningPeriodJpaEntity> findByYear(Integer year);

    List<AllocationPlanningPeriodJpaEntity> findByYearAndStatus(Integer year, String status);

    List<AllocationPlanningPeriodJpaEntity> findByStatus(String status);

    @Query("SELECT p FROM AllocationPlanningPeriodJpaEntity p WHERE p.year = :year AND :weekNumber BETWEEN p.startWeek AND p.endWeek AND p.status = 'LOCKED'")
    List<AllocationPlanningPeriodJpaEntity> findLockedPeriodsCoveringWeek(@Param("year") int year, @Param("weekNumber") int weekNumber);

    @Query("SELECT COUNT(p) > 0 FROM AllocationPlanningPeriodJpaEntity p WHERE p.year = :year AND ((p.startWeek BETWEEN :startWeek AND :endWeek) OR (p.endWeek BETWEEN :startWeek AND :endWeek) OR (:startWeek BETWEEN p.startWeek AND p.endWeek)) AND (:excludeId IS NULL OR p.id <> :excludeId)")
    boolean existsOverlapping(
            @Param("year") Integer year,
            @Param("startWeek") int startWeek,
            @Param("endWeek") int endWeek,
            @Param("excludeId") Long excludeId
    );
}
