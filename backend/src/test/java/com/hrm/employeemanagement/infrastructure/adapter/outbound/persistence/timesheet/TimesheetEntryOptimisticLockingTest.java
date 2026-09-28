package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.timesheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetEntryRepository;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.TimesheetEntryJpaEntity;

@SpringBootTest
@ActiveProfiles("test")
class TimesheetEntryOptimisticLockingTest {

    @Autowired
    private SpringDataTimesheetEntryRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Optimistic locking: stale TimesheetEntry update is rejected at persistence layer")
    void rejectsStaleConcurrentUpdateAtPersistenceLayer() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long entryId = null;

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            entryId = transaction.execute(status -> {
                TimesheetEntryJpaEntity entity = new TimesheetEntryJpaEntity();
                entity.setTimesheetId(900001L);
                entity.setEmployeeId(900001L);
                entity.setProjectId(900001L);
                entity.setTaskId(900001L);
                entity.setWorkDate(LocalDate.of(2026, 9, 20));
                entity.setHours(new BigDecimal("8.00"));
                entity.setBillable(true);
                entity.setDescription("Initial approved work log");
                entity.setStatus("APPROVED");
                return repository.saveAndFlush(entity).getId();
            });

            assertNotNull(entryId);
            Long persistedEntryId = entryId;

            TimesheetEntryJpaEntity staleEntry = transaction.execute(status ->
                    repository.findById(persistedEntryId).orElseThrow());
            assertNotNull(staleEntry);
            assertEquals(0L, staleEntry.getVersion());

            transaction.executeWithoutResult(status -> {
                TimesheetEntryJpaEntity concurrentEntry = repository.findById(persistedEntryId).orElseThrow();
                concurrentEntry.setHours(new BigDecimal("7.00"));
                repository.saveAndFlush(concurrentEntry);
            });

            staleEntry.setHours(new BigDecimal("6.00"));
            assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                    transaction.executeWithoutResult(status -> repository.saveAndFlush(staleEntry)));

            BigDecimal persistedHours = transaction.execute(status ->
                    repository.findById(persistedEntryId).orElseThrow().getHours());
            assertEquals(0, new BigDecimal("7.00").compareTo(persistedHours));
        } finally {
            if (entryId != null) {
                jdbcTemplate.update("DELETE FROM timesheet_entries WHERE id = ?", entryId);
            }
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
