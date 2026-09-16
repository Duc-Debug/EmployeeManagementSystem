package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleConflictOptimisticLockingTest {

    @Autowired
    private SpringDataScheduleConflictRepository repository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Optimistic Locking: Cập nhật đồng thời bản ghi cũ throw JpaOptimisticLockingFailureException")
    void testOptimisticLockingFailureOnConcurrentStaleUpdate() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // Step 0: Create a valid employee in DB
        Long employeeId = transactionTemplate.execute(status -> {
            EmployeeJpaEntity emp = new EmployeeJpaEntity();
            emp.setEmployeeCode("EMP-OPT-LOCK-" + System.currentTimeMillis());
            emp.setFullName("Test Optimistic Employee");
            emp.setStatus("ACTIVE");
            emp.setStandardHoursPerWeek(40);
            emp.setIsOutsourced(false);
            return employeeRepository.save(emp).getId();
        });

        assertNotNull(employeeId);

        // Step 1: Create & save initial entity in Tx 1
        Long conflictId = transactionTemplate.execute(status -> {
            ScheduleConflictJpaEntity entity = new ScheduleConflictJpaEntity();
            entity.setEmployeeId(employeeId);
            entity.setYearNumber(2026);
            entity.setWeekNumber(40);
            entity.setConflictType(ConflictType.MULTI_PROJECT_ALLOCATION);
            entity.setTotalAllocatedHours(BigDecimal.valueOf(80.0));
            entity.setNetAvailableHours(BigDecimal.valueOf(40.0));
            entity.setExcessHours(BigDecimal.valueOf(40.0));
            entity.setStatus(ScheduleConflictStatus.OPEN);
            return repository.saveAndFlush(entity).getId();
        });

        assertNotNull(conflictId);

        // Step 2: Tx 2 loads entity (version 0)
        ScheduleConflictJpaEntity entityTx2 = transactionTemplate.execute(status ->
                repository.findById(conflictId).orElseThrow()
        );
        assertNotNull(entityTx2);
        assertEquals(0L, entityTx2.getVersion());

        // Step 3: Tx 3 loads entity (version 0), updates & commits -> version becomes 1
        transactionTemplate.execute(status -> {
            ScheduleConflictJpaEntity entityTx3 = repository.findById(conflictId).orElseThrow();
            entityTx3.setResolutionNote("Cập nhật bởi Tx 3");
            repository.saveAndFlush(entityTx3);
            return null;
        });

        // Step 4: Tx 4 attempts to save entityTx2 (stale version 0) -> expects Optimistic Locking failure
        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
            transactionTemplate.execute(status -> {
                entityTx2.setResolutionNote("Cập nhật bởi Tx 2 (Stale)");
                repository.saveAndFlush(entityTx2);
                return null;
            });
        });
    }
}
