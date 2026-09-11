package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskAssignmentJpaEntity;

@Repository
public interface SpringDataTaskAssignmentRepository extends JpaRepository<TaskAssignmentJpaEntity, Long> {

    List<TaskAssignmentJpaEntity> findByTaskId(Long taskId);

    List<TaskAssignmentJpaEntity> findByTaskIdIn(List<Long> taskIds);

    List<TaskAssignmentJpaEntity> findByEmployeeId(Long employeeId);

    Optional<TaskAssignmentJpaEntity> findByTaskIdAndEmployeeId(Long taskId, Long employeeId);

    @Modifying
    @Query("DELETE FROM TaskAssignmentJpaEntity ta WHERE ta.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM TaskAssignmentJpaEntity ta WHERE ta.taskId = :taskId AND ta.employeeId NOT IN :employeeIds")
    void deleteByTaskIdAndEmployeeIdNotIn(@Param("taskId") Long taskId, @Param("employeeIds") List<Long> employeeIds);
}

