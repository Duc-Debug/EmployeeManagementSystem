package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.ScheduleConfirmationJpaEntity;

@Repository
public interface SpringDataScheduleConfirmationRepository extends JpaRepository<ScheduleConfirmationJpaEntity, Long> {

    Optional<ScheduleConfirmationJpaEntity> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    List<ScheduleConfirmationJpaEntity> findByUserIdAndWeekStartDateIn(Long userId, List<LocalDate> weekStartDates);
}