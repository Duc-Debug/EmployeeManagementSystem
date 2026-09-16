package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Import(ScheduleConflictConcurrentScanTest.TransactionalScanTestService.class)
class ScheduleConflictConcurrentScanTest {

    @Autowired
    private TransactionalScanTestService transactionalTestService;

    @Autowired
    private SpringDataScheduleConflictRepository repository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Long> createdEmployeeIds = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            repository.deleteAll();
            for (Long id : createdEmployeeIds) {
                employeeRepository.deleteById(id);
            }
        });
        createdEmployeeIds.clear();
    }

    @Test
    @DisplayName("Concurrent Scan trong @Transactional: Quét đồng thời 2 thread không gây UnexpectedRollbackException hay HTTP 500")
    void testConcurrentScansInsideTransactionalServiceHandledGracefully() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        Long employeeId = transactionTemplate.execute(status -> {
            EmployeeJpaEntity emp = new EmployeeJpaEntity();
            emp.setEmployeeCode("EMP-CONC-TX-" + System.currentTimeMillis());
            emp.setFullName("Concurrent Transactional Scan Employee");
            emp.setStatus("ACTIVE");
            emp.setStandardHoursPerWeek(40);
            emp.setIsOutsourced(false);
            return employeeRepository.save(emp).getId();
        });

        assertNotNull(employeeId);
        createdEmployeeIds.add(employeeId);

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    ScheduleConflict conflict = ScheduleConflict.create(
                            employeeId, 2026, 42, ConflictType.MULTI_PROJECT_ALLOCATION,
                            "1,2", "Dự án Alpha, Dự án Beta", null, null,
                            BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                            "Phân bổ bởi Thread " + threadNum
                    );
                    // Executed inside an active @Transactional boundary
                    transactionalTestService.scanInTransaction(conflict);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release both threads simultaneously
        boolean finished = finishLatch.await(10, TimeUnit.SECONDS);

        executor.shutdown();

        assertTrue(finished, "Tất cả các thread hoàn thành trong thời gian quy định");
        assertEquals(0, exceptionCount.get(), "Không được có UnexpectedRollbackException hay Exception (500) nào xảy ra khi scan đồng thời");
        assertEquals(2, successCount.get(), "Cả 2 thread đều hoàn thành thành công nhờ cơ chế REQUIRES_NEW sub-transaction");

        long countInDb = (long) transactionTemplate.execute(status ->
                repository.findConflicts(2026, 42, 42, employeeId, ConflictType.MULTI_PROJECT_ALLOCATION, null).size()
        );
        assertEquals(1L, countInDb, "DB chỉ lưu đúng 1 bản ghi duy nhất cho employeeId + year + week + conflictType");
    }

    @Component
    public static class TransactionalScanTestService {

        private final ScheduleConflictPersistenceAdapter persistenceAdapter;

        public TransactionalScanTestService(ScheduleConflictPersistenceAdapter persistenceAdapter) {
            this.persistenceAdapter = persistenceAdapter;
        }

        @Transactional
        public List<ScheduleConflict> scanInTransaction(ScheduleConflict conflict) {
            return persistenceAdapter.saveAll(List.of(conflict));
        }
    }
}
