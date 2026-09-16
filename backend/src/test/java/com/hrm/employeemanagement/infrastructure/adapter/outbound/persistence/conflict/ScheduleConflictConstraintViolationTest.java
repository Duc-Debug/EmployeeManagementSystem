package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict;

import java.math.BigDecimal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleConflictConstraintViolationTest {

    @Autowired
    private ScheduleConflictPersistenceAdapter adapter;

    @Autowired
    private SpringDataScheduleConflictRepository conflictRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Long> createdEmployeeIds = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            conflictRepository.deleteAll();
            for (Long id : createdEmployeeIds) {
                employeeRepository.deleteById(id);
            }
        });
        createdEmployeeIds.clear();
    }

    @Test
    @DisplayName("Regression Test: Foreign Key violation phải throw DataIntegrityViolationException, KHÔNG được nuốt lỗi")
    void testForeignKeyViolationRethrown() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        Long employeeId = transactionTemplate.execute(status -> {
            EmployeeJpaEntity emp = new EmployeeJpaEntity();
            emp.setEmployeeCode("EMP-FK-TEST-" + System.currentTimeMillis());
            emp.setFullName("FK Test Employee");
            emp.setStatus("ACTIVE");
            emp.setStandardHoursPerWeek(40);
            emp.setIsOutsourced(false);
            return employeeRepository.save(emp).getId();
        });

        assertNotNull(employeeId);
        createdEmployeeIds.add(employeeId);

        // Create a conflict with an invalid assigned_handler_id (999999L does not exist)
        ScheduleConflict conflictWithInvalidFk = ScheduleConflict.create(
                employeeId, 2026, 45, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1", "Dự án Alpha", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Conflict với FK sai"
        );
        conflictWithInvalidFk.assignHandler(999999L); // Non-existent employee ID -> FK violation

        // Verification: Must throw DataIntegrityViolationException because it is a FK violation, not a unique key duplicate
        assertThrows(DataIntegrityViolationException.class, () -> {
            adapter.save(conflictWithInvalidFk);
        }, "Lỗi FK violation (assigned_handler_id không tồn tại) phải được throw tiếp, không được nuốt exception");
    }

    @Test
    @DisplayName("Unique Key collision: Duplicate (employee_id, year, week, conflict_type) sẽ được fallback xử lý thành công")
    void testUniqueKeyViolationHandledAsFallback() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        Long employeeId = transactionTemplate.execute(status -> {
            EmployeeJpaEntity emp = new EmployeeJpaEntity();
            emp.setEmployeeCode("EMP-UK-TEST-" + System.currentTimeMillis());
            emp.setFullName("UK Test Employee");
            emp.setStatus("ACTIVE");
            emp.setStandardHoursPerWeek(40);
            emp.setIsOutsourced(false);
            return employeeRepository.save(emp).getId();
        });

        assertNotNull(employeeId);
        createdEmployeeIds.add(employeeId);

        ScheduleConflict conflict1 = ScheduleConflict.create(
                employeeId, 2026, 46, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1", "Dự án Alpha", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Conflict lần 1"
        );

        ScheduleConflict saved1 = adapter.save(conflict1);
        assertNotNull(saved1.getId());

        // Duplicate conflict on same employee, year, week, conflict_type
        ScheduleConflict conflict2 = ScheduleConflict.create(
                employeeId, 2026, 46, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(50.0),
                "Conflict lần 2 (Trùng Unique Key)"
        );

        ScheduleConflict saved2 = assertDoesNotThrow(() -> adapter.save(conflict2));
        assertEquals(saved1.getId(), saved2.getId(), "Duplicate unique key sẽ fallback update lại bản ghi cũ có cùng ID");
    }

    @Test
    @DisplayName("Regression Test: Unique constraint khác không thuộc uk_schedule_conflict_existing phải được rethrow, không được kích hoạt fallback")
    void testUnrelatedUniqueConstraintViolationRethrown() {
        TransactionalConflictSaveHelper mockHelper = org.mockito.Mockito.mock(TransactionalConflictSaveHelper.class);
        ScheduleConflictPersistenceAdapter adapterWithMock = new ScheduleConflictPersistenceAdapter(
                org.mockito.Mockito.mock(com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictRepository.class),
                mockHelper
        );

        ScheduleConflict conflict = ScheduleConflict.create(
                1L, 2026, 47, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1", "Dự án Alpha", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Unrelated Unique Test"
        );

        // Simulate an unrelated constraint exception (e.g. UK_SOME_OTHER_FIELD)
        org.hibernate.exception.ConstraintViolationException hibernateExc = new org.hibernate.exception.ConstraintViolationException(
                "Duplicate entry for key 'uk_some_other_field'", null, "uk_some_other_field"
        );
        DataIntegrityViolationException unrelatedUniqueException = new DataIntegrityViolationException("Duplicate entry for key 'uk_some_other_field'", hibernateExc);

        org.mockito.Mockito.when(mockHelper.saveAndFlushRequiresNew(org.mockito.Mockito.any()))
                .thenThrow(unrelatedUniqueException);

        // Verification: Must rethrow DataIntegrityViolationException and NEVER call updateExistingRequiresNew
        assertThrows(DataIntegrityViolationException.class, () -> adapterWithMock.save(conflict));
        org.mockito.Mockito.verify(mockHelper, org.mockito.Mockito.never())
                .updateExistingRequiresNew(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyInt(), org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("Regression Test: Fallback update phải giữ nguyên lifecycle fields (status RESOLVED, assignedHandler, resolutionNote, resolvedBy) và chỉ update scan fields")
    void testResolvedLifecyclePreservedOnUniqueFallback() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        Long employeeId = transactionTemplate.execute(status -> {
            EmployeeJpaEntity emp = new EmployeeJpaEntity();
            emp.setEmployeeCode("EMP-RESOLVE-PRESERVE-" + System.currentTimeMillis());
            emp.setFullName("Resolve Preservation Employee");
            emp.setStatus("ACTIVE");
            emp.setStandardHoursPerWeek(40);
            emp.setIsOutsourced(false);
            return employeeRepository.save(emp).getId();
        });

        Long handlerId = transactionTemplate.execute(status -> {
            EmployeeJpaEntity handler = new EmployeeJpaEntity();
            handler.setEmployeeCode("EMP-HANDLER-" + System.currentTimeMillis());
            handler.setFullName("Handler Employee");
            handler.setStatus("ACTIVE");
            handler.setStandardHoursPerWeek(40);
            handler.setIsOutsourced(false);
            return employeeRepository.save(handler).getId();
        });

        assertNotNull(employeeId);
        assertNotNull(handlerId);
        createdEmployeeIds.add(employeeId);
        createdEmployeeIds.add(handlerId);

        // 1. Tạo bản ghi conflict và giải quyết (RESOLVED) với assignedHandlerId, resolutionNote, resolvedBy
        ScheduleConflict conflict1 = ScheduleConflict.create(
                employeeId, 2026, 48, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1", "Dự án Alpha", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Conflict ban đầu"
        );
        conflict1.assignHandler(handlerId);
        conflict1.resolveWithNote(handlerId, handlerId, "Đã thương lượng giải quyết xung đột thành công");

        ScheduleConflict resolvedSaved = adapter.save(conflict1);
        assertEquals(com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus.RESOLVED, resolvedSaved.getStatus());
        assertEquals(handlerId, resolvedSaved.getAssignedHandlerId());
        assertEquals("Đã thương lượng giải quyết xung đột thành công", resolvedSaved.getResolutionNote());
        assertNotNull(resolvedSaved.getResolvedAt());
        assertEquals(handlerId, resolvedSaved.getResolvedBy());

        // 2. Scan tạo ra một new conflict (chưa có ID) bị trùng unique key (employeeId, 2026, 48, MULTI_PROJECT_ALLOCATION)
        ScheduleConflict duplicateScanConflict = ScheduleConflict.create(
                employeeId, 2026, 48, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(50.0),
                "Scan tính toán lại dữ liệu mới"
        );

        // 3. Thực hiện save (kích hoạt fallback update)
        ScheduleConflict fallbackResult = assertDoesNotThrow(() -> adapter.save(duplicateScanConflict));

        // 4. Verification:
        assertEquals(resolvedSaved.getId(), fallbackResult.getId());
        assertEquals(com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus.RESOLVED, fallbackResult.getStatus(), "Status vẫn phải là RESOLVED");
        assertEquals(handlerId, fallbackResult.getAssignedHandlerId(), "Assigned handler phải được giữ nguyên");
        assertEquals("Đã thương lượng giải quyết xung đột thành công", fallbackResult.getResolutionNote(), "Resolution note phải được giữ nguyên");
        assertEquals(resolvedSaved.getResolvedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                fallbackResult.getResolvedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                "ResolvedAt phải được giữ nguyên");
        assertEquals(handlerId, fallbackResult.getResolvedBy(), "ResolvedBy phải được giữ nguyên");
        assertEquals("1,2", fallbackResult.getProjectIds());
        assertEquals("Dự án Alpha, Dự án Beta", fallbackResult.getProjectNames());
        assertEquals(BigDecimal.valueOf(90.0), fallbackResult.getTotalAllocatedHours());
        assertEquals(BigDecimal.valueOf(50.0), fallbackResult.getExcessHours());
        assertEquals("Scan tính toán lại dữ liệu mới", fallbackResult.getDetails());
    }

    @Test
    @DisplayName("Batch save: saveAll lưu thành công nhiều conflict bằng batch execution")
    void testBatchSaveSuccess() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        Long employeeId = transactionTemplate.execute(status -> {
            EmployeeJpaEntity emp = new EmployeeJpaEntity();
            emp.setEmployeeCode("EMP-BATCH-" + System.currentTimeMillis());
            emp.setFullName("Batch Employee");
            emp.setStatus("ACTIVE");
            emp.setStandardHoursPerWeek(40);
            emp.setIsOutsourced(false);
            return employeeRepository.save(emp).getId();
        });

        assertNotNull(employeeId);
        createdEmployeeIds.add(employeeId);

        java.util.List<ScheduleConflict> batch = java.util.List.of(
                ScheduleConflict.create(employeeId, 2026, 1, ConflictType.MULTI_PROJECT_ALLOCATION, "1", "Project 1", null, null, BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(20.0), "Week 1"),
                ScheduleConflict.create(employeeId, 2026, 2, ConflictType.MULTI_PROJECT_ALLOCATION, "1", "Project 1", null, null, BigDecimal.valueOf(70.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(30.0), "Week 2"),
                ScheduleConflict.create(employeeId, 2026, 3, ConflictType.MULTI_PROJECT_ALLOCATION, "1", "Project 1", null, null, BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0), "Week 3")
        );

        java.util.List<ScheduleConflict> savedBatch = assertDoesNotThrow(() -> adapter.saveAll(batch));
        assertEquals(3, savedBatch.size());
        savedBatch.forEach(saved -> assertNotNull(saved.getId()));
    }
}
