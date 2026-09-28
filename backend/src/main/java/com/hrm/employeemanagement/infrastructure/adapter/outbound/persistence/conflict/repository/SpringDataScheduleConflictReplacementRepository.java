package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictReplacementJpaEntity;

public interface SpringDataScheduleConflictReplacementRepository extends JpaRepository<ScheduleConflictReplacementJpaEntity, Long> {
    List<ScheduleConflictReplacementJpaEntity> findByConflictId(Long conflictId);
}
