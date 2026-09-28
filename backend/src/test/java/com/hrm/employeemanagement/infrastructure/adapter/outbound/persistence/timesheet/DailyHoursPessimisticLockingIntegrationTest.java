package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.timesheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetEntryPort;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetEntryRepository;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.TimesheetEntryJpaEntity;

@SpringBootTest
@ActiveProfiles("test")
class DailyHoursPessimisticLockingIntegrationTest {

    private static final LocalDate WORK_DATE = LocalDate.of(2026, 9, 21);

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataTimesheetEntryRepository entryRepository;

    @Autowired
    private SaveTimesheetEntryPort saveTimesheetEntryPort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Pessimistic lock serializes two daily-hour updates and keeps final total at 12h")
    void serializesConcurrentUpdatesAndRejectsTheSecondOne() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstHasLock = new CountDownLatch(1);
        CountDownLatch secondAttemptsSave = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);

        Long employeeId = null;
        Long entryAId = null;
        Long entryBId = null;
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            EmployeeJpaEntity employee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                    null, null, null, "LOCK-TEST-" + System.nanoTime(), "Lock Test Employee",
                    "Developer", LocalDate.of(2024, 1, 1), null, false, 40, "ACTIVE"));
            employeeId = employee.getId();
            entryAId = persistEntry(employeeId, 910001L, new BigDecimal("5.00"));
            entryBId = persistEntry(employeeId, 910002L, new BigDecimal("5.00"));

            Long finalEmployeeId = employeeId;
            Long finalEntryAId = entryAId;
            Long finalEntryBId = entryBId;

            CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> transaction.execute(status -> {
                employeeRepository.findByIdForUpdate(finalEmployeeId).orElseThrow();
                firstHasLock.countDown();
                await(allowFirstCommit);
                saveTimesheetEntryPort.save(domainEntry(finalEntryAId, finalEmployeeId, 910001L, "7.00", 0L));
                return "SUCCESS";
            }), executor);

            assertTrue(firstHasLock.await(5, TimeUnit.SECONDS));

            CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> {
                try {
                    return transaction.execute(status -> {
                        secondAttemptsSave.countDown();
                        saveTimesheetEntryPort.save(domainEntry(finalEntryBId, finalEmployeeId, 910002L, "7.00", 0L));
                        return "SUCCESS";
                    });
                } catch (DailyHoursLimitExceededException exception) {
                    return "DAILY_LIMIT";
                }
            }, executor);

            assertTrue(secondAttemptsSave.await(5, TimeUnit.SECONDS));
            Thread.sleep(200); // The second transaction must still be waiting on the employee row.
            assertFalse(second.isDone());

            allowFirstCommit.countDown();
            List<String> outcomes = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertTrue(outcomes.contains("SUCCESS"));
            assertTrue(outcomes.contains("DAILY_LIMIT"));

            BigDecimal finalTotal = entryRepository.sumHoursByEmployeeIdAndWorkDate(employeeId, WORK_DATE, null);
            assertEquals(0, new BigDecimal("12.00").compareTo(finalTotal));
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
            if (entryAId != null) jdbcTemplate.update("DELETE FROM timesheet_entries WHERE id = ?", entryAId);
            if (entryBId != null) jdbcTemplate.update("DELETE FROM timesheet_entries WHERE id = ?", entryBId);
            if (employeeId != null) jdbcTemplate.update("DELETE FROM employees WHERE id = ?", employeeId);
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private Long persistEntry(Long employeeId, Long taskId, BigDecimal hours) {
        TimesheetEntryJpaEntity entity = new TimesheetEntryJpaEntity();
        entity.setTimesheetId(920001L);
        entity.setEmployeeId(employeeId);
        entity.setProjectId(930001L);
        entity.setTaskId(taskId);
        entity.setWorkDate(WORK_DATE);
        entity.setHours(hours);
        entity.setBillable(true);
        entity.setDescription("Concurrent lock test");
        entity.setStatus("APPROVED");
        return entryRepository.saveAndFlush(entity).getId();
    }

    private TimesheetEntry domainEntry(Long id, Long employeeId, Long taskId, String hours, Long version) {
        return new TimesheetEntry(
                new TimesheetEntryId(id), new TimesheetId(920001L), new EmployeeId(employeeId),
                new ProjectId(930001L), new TaskId(taskId), WORK_DATE, new BigDecimal(hours),
                true, "Concurrent lock test", TimesheetStatus.APPROVED, null, null, version);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test interrupted", exception);
        }
    }
}
