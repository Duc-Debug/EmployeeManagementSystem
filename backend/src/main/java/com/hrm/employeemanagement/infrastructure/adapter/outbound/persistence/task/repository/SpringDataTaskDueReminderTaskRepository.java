package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;

/**
 * Spring Data JPA Repository chuyên biệt cho việc truy vấn công việc sắp đến hạn (NCL-11-CN-004).
 * Độc lập, không sửa đổi hay ảnh hưởng đến code của repository khác.
 */
@Repository
public interface SpringDataTaskDueReminderTaskRepository extends JpaRepository<TaskJpaEntity, Long> {

    @Query("SELECT t FROM TaskJpaEntity t WHERE t.assigneeId IS NOT NULL " +
           "AND t.status NOT IN :excludedStatuses " +
           "AND ((t.dueDate IS NOT NULL AND t.dueDate BETWEEN :fromDate AND :toDate) " +
           "     OR (t.dueDate IS NULL AND t.plannedEndDate IS NOT NULL AND t.plannedEndDate BETWEEN :fromDate AND :toDate)) " +
           "ORDER BY COALESCE(t.dueDate, t.plannedEndDate) ASC")
    List<TaskJpaEntity> findTasksDueBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("excludedStatuses") Collection<TaskStatus> excludedStatuses
    );

    @Query("SELECT t FROM TaskJpaEntity t WHERE t.assigneeId = :assigneeId " +
           "AND t.status NOT IN :excludedStatuses " +
           "AND ((t.dueDate IS NOT NULL AND t.dueDate BETWEEN :fromDate AND :toDate) " +
           "     OR (t.dueDate IS NULL AND t.plannedEndDate IS NOT NULL AND t.plannedEndDate BETWEEN :fromDate AND :toDate)) " +
           "ORDER BY COALESCE(t.dueDate, t.plannedEndDate) ASC")
    List<TaskJpaEntity> findUpcomingTasksByAssignee(
            @Param("assigneeId") Long assigneeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("excludedStatuses") Collection<TaskStatus> excludedStatuses
    );
}
