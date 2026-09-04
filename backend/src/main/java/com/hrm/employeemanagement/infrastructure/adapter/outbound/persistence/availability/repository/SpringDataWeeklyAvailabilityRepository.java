package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.WeeklyAvailabilityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataWeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailabilityJpaEntity, Long> {
    Optional<WeeklyAvailabilityJpaEntity> findByEmployeeIdAndYearAndWeekNumber(Long employeeId, Integer year, Integer weekNumber);
}
