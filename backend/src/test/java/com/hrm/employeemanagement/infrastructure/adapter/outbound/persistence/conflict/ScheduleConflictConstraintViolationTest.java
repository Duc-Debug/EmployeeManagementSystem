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
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictJpaEntity;
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
    @DisplayName("Unique Key collision: Duplicate uk_schedule_conflict_existing sẽ được fallback xử lý thành công")
    void testUniqueKeyViolationHandledAsFallback() {
        TransactionalConflictSaveHelper mockHelper = org.mockito.Mockito.mock(TransactionalConflictSaveHelper.class);
        ScheduleConflictPersistenceAdapter adapterWithMock = new ScheduleConflictPersistenceAdapter(
                org.mockito.Mockito.mock(SpringDataScheduleConflictRepository.class),
                mockHelper
        );

        ScheduleConflict conflict = ScheduleConflict.create(
                1L, 2026, 46, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(50.0),
                "Conflict lần 2 (Trùng Unique Key)"
        );

        org.hibernate.exception.ConstraintViolationException hibernateExc = new org.hibernate.exception.ConstraintViolationException(
                "Duplicate entry for key 'uk_schedule_conflict_existing'", null, "uk_schedule_conflict_existing"
        );
        DataIntegrityViolationException ukException = new DataIntegrityViolationException("Duplicate entry for key 'uk_schedule_conflict_existing'", hibernateExc);

        org.mockito.Mockito.when(mockHelper.saveAndFlushRequiresNew(org.mockito.Mockito.any()))
                .thenThrow(ukException);

        ScheduleConflictJpaEntity mockUpdated = new ScheduleConflictJpaEntity();
        mockUpdated.setId(10L);
        mockUpdated.setEmployeeId(1L);
        mockUpdated.setYearNumber(2026);
        mockUpdated.setWeekNumber(46);
        mockUpdated.setConflictType(ConflictType.MULTI_PROJECT_ALLOCATION);
        mockUpdated.setStatus(com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus.OPEN);

        org.mockito.Mockito.when(mockHelper.updateExistingRequiresNew(
                org.mockito.Mockito.eq(1L),
                org.mockito.Mockito.eq(2026),
                org.mockito.Mockito.eq(46),
                org.mockito.Mockito.eq(ConflictType.MULTI_PROJECT_ALLOCATION),
                org.mockito.Mockito.any()
        )).thenReturn(mockUpdated);

        ScheduleConflict saved = assertDoesNotThrow(() -> adapterWithMock.save(conflict));
        assertNotNull(saved);
        assertEquals(10L, saved.getId());
        org.mockito.Mockito.verify(mockHelper).updateExistingRequiresNew(
                org.mockito.Mockito.eq(1L),
                org.mockito.Mockito.eq(2026),
                org.mockito.Mockito.eq(46),
                org.mockito.Mockito.eq(ConflictType.MULTI_PROJECT_ALLOCATION),
                org.mockito.Mockito.any()
        );
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

        TransactionalConflictSaveHelper helper = new TransactionalConflictSaveHelper(conflictRepository);

        // 1. Tạo bản ghi conflict ban đầu trong DB và giải quyết (RESOLVED)
        ScheduleConflictJpaEntity initialEntity = new ScheduleConflictJpaEntity();
        initialEntity.setEmployeeId(employeeId);
        initialEntity.setYearNumber(2026);
        initialEntity.setWeekNumber(48);
        initialEntity.setConflictType(ConflictType.MULTI_PROJECT_ALLOCATION);
        initialEntity.setProjectIds("1");
        initialEntity.setProjectNames("Dự án Alpha");
        initialEntity.setTotalAllocatedHours(BigDecimal.valueOf(80.0));
        initialEntity.setNetAvailableHours(BigDecimal.valueOf(40.0));
        initialEntity.setExcessHours(BigDecimal.valueOf(40.0));
        initialEntity.setStatus(com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus.RESOLVED);
        initialEntity.setAssignedHandlerId(handlerId);
        initialEntity.setResolutionNote("Đã thương lượng giải quyết xung đột thành công");
        initialEntity.setResolvedAt(java.time.LocalDateTime.now());
        initialEntity.setResolvedBy(handlerId);
        initialEntity.setDetails("Conflict ban đầu");

        ScheduleConflictJpaEntity savedInitial = helper.saveAndFlushRequiresNew(initialEntity);
        assertNotNull(savedInitial.getId());

        // 2. Scan tạo entity mới với cùng unique key (employeeId, 2026, 48, MULTI_PROJECT_ALLOCATION) nhưng data mới
        ScheduleConflictJpaEntity scanNewData = new ScheduleConflictJpaEntity();
        scanNewData.setEmployeeId(employeeId);
        scanNewData.setYearNumber(2026);
        scanNewData.setWeekNumber(48);
        scanNewData.setConflictType(ConflictType.MULTI_PROJECT_ALLOCATION);
        scanNewData.setProjectIds("1,2");
        scanNewData.setProjectNames("Dự án Alpha, Dự án Beta");
        scanNewData.setTotalAllocatedHours(BigDecimal.valueOf(90.0));
        scanNewData.setNetAvailableHours(BigDecimal.valueOf(40.0));
        scanNewData.setExcessHours(BigDecimal.valueOf(50.0));
        scanNewData.setStatus(com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus.OPEN);
        scanNewData.setDetails("Scan tính toán lại dữ liệu mới");

        // 3. Gọi helper updateExistingRequiresNew
        ScheduleConflictJpaEntity updated = helper.updateExistingRequiresNew(
                employeeId, 2026, 48, ConflictType.MULTI_PROJECT_ALLOCATION,
                scanNewData
        );

        // 4. Verification: Lifecycle fields giữ nguyên, scan fields được cập nhật
        assertEquals(savedInitial.getId(), updated.getId());
        assertEquals(com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus.RESOLVED, updated.getStatus(), "Status vẫn phải là RESOLVED");
        assertEquals(handlerId, updated.getAssignedHandlerId(), "Assigned handler phải được giữ nguyên");
        assertEquals("Đã thương lượng giải quyết xung đột thành công", updated.getResolutionNote(), "Resolution note phải được giữ nguyên");
        assertEquals(savedInitial.getResolvedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                updated.getResolvedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                "ResolvedAt phải được giữ nguyên");
        assertEquals(handlerId, updated.getResolvedBy(), "ResolvedBy phải được giữ nguyên");
        assertEquals("1,2", updated.getProjectIds());
        assertEquals("Dự án Alpha, Dự án Beta", updated.getProjectNames());
        assertEquals(BigDecimal.valueOf(90.0), updated.getTotalAllocatedHours());
        assertEquals(BigDecimal.valueOf(50.0), updated.getExcessHours());
        assertEquals("Scan tính toán lại dữ liệu mới", updated.getDetails());
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
