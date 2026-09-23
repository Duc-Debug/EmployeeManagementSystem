package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyProjectAllocationPersistenceAdapter Optimistic Locking Tests")
class WeeklyProjectAllocationPersistenceAdapterTest {

    @Mock
    private SpringDataWeeklyProjectAllocationRepository repository;

    private WeeklyProjectAllocationPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WeeklyProjectAllocationPersistenceAdapter(repository);
    }

    @Nested
    @DisplayName("Save Optimistic Locking Tests")
    class SaveOptimisticLockingTests {

        @Test
        @DisplayName("Should successfully save when version matches DB version including projectRoleId")
        void shouldSaveWhenVersionMatches() {
            Long allocationId = 100L;
            WeeklyProjectAllocation domain = new WeeklyProjectAllocation(
                    allocationId, 1L, 2L, 5L, YearWeek.of(2026, 10),
                    BigDecimal.valueOf(30), BigDecimal.valueOf(75),
                    false, null, null, null, "Note", 10L, 5L
            );

            WeeklyProjectAllocationJpaEntity dbEntity = new WeeklyProjectAllocationJpaEntity(
                    allocationId, 1L, 2L, 4L, 2026, 10,
                    BigDecimal.valueOf(20), BigDecimal.valueOf(50),
                    false, null, null, null, null, null, 5L
            );

            when(repository.findById(allocationId)).thenReturn(Optional.of(dbEntity));
            when(repository.save(any(WeeklyProjectAllocationJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WeeklyProjectAllocation saved = adapter.save(domain);

            assertNotNull(saved);
            assertEquals(5L, saved.getProjectRoleId());
            assertEquals(BigDecimal.valueOf(30), saved.getAllocatedHours());
            assertEquals("Note", saved.getVarianceNote());
            verify(repository).save(dbEntity);
            assertEquals(5L, dbEntity.getProjectRoleId());
        }

        @Test
        @DisplayName("Should insert new allocation with projectRoleId")
        void shouldInsertNewAllocationWithProjectRoleId() {
            WeeklyProjectAllocation domain = WeeklyProjectAllocation.createNew(
                    1L, 2L, 5L, YearWeek.of(2026, 10),
                    BigDecimal.valueOf(40), BigDecimal.valueOf(100)
            );

            WeeklyProjectAllocationJpaEntity savedEntity = new WeeklyProjectAllocationJpaEntity(
                    200L, 1L, 2L, 5L, 2026, 10,
                    BigDecimal.valueOf(40), BigDecimal.valueOf(100), 0L
            );

            when(repository.save(any(WeeklyProjectAllocationJpaEntity.class))).thenReturn(savedEntity);

            WeeklyProjectAllocation saved = adapter.save(domain);

            assertNotNull(saved);
            assertEquals(200L, saved.getId());
            assertEquals(5L, saved.getProjectRoleId());
        }

        @Test
        @DisplayName("Should throw ObjectOptimisticLockingFailureException when domain version does not match DB version")
        void shouldThrowWhenVersionMismatch() {
            Long allocationId = 100L;
            WeeklyProjectAllocation domain = new WeeklyProjectAllocation(
                    allocationId, 1L, 2L, YearWeek.of(2026, 10),
                    BigDecimal.valueOf(40), BigDecimal.valueOf(100),
                    false, null, null, null, null, null, 5L
            );

            // DB already progressed to version 6
            WeeklyProjectAllocationJpaEntity dbEntity = new WeeklyProjectAllocationJpaEntity(
                    allocationId, 1L, 2L, 2026, 10,
                    BigDecimal.valueOf(30), BigDecimal.valueOf(75),
                    false, null, null, null, null, null, 6L
            );

            when(repository.findById(allocationId)).thenReturn(Optional.of(dbEntity));

            assertThrows(ObjectOptimisticLockingFailureException.class, () -> adapter.save(domain));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ObjectOptimisticLockingFailureException when existing allocation was deleted from DB")
        void shouldThrowWhenRecordDeletedConcurrently() {
            Long allocationId = 100L;
            WeeklyProjectAllocation domain = new WeeklyProjectAllocation(
                    allocationId, 1L, 2L, YearWeek.of(2026, 10),
                    BigDecimal.valueOf(30), BigDecimal.valueOf(75),
                    5L
            );

            when(repository.findById(allocationId)).thenReturn(Optional.empty());

            assertThrows(ObjectOptimisticLockingFailureException.class, () -> adapter.save(domain));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Optimistic Locking Tests")
    class DeleteOptimisticLockingTests {

        @Test
        @DisplayName("Should successfully delete when version matches DB version")
        void shouldDeleteWhenVersionMatches() {
            Long allocationId = 100L;
            WeeklyProjectAllocation domain = new WeeklyProjectAllocation(
                    allocationId, 1L, 2L, YearWeek.of(2026, 10),
                    BigDecimal.valueOf(20), BigDecimal.valueOf(50),
                    false, null, null, null, null, null, 3L
            );

            WeeklyProjectAllocationJpaEntity dbEntity = new WeeklyProjectAllocationJpaEntity(
                    allocationId, 1L, 2L, 2026, 10,
                    BigDecimal.valueOf(20), BigDecimal.valueOf(50),
                    false, null, null, null, null, null, 3L
            );

            when(repository.findById(allocationId)).thenReturn(Optional.of(dbEntity));

            adapter.delete(domain);

            verify(repository).delete(dbEntity);
        }

        @Test
        @DisplayName("Should throw ObjectOptimisticLockingFailureException when deleting with outdated version")
        void shouldThrowWhenDeleteVersionMismatch() {
            Long allocationId = 100L;
            WeeklyProjectAllocation domain = new WeeklyProjectAllocation(
                    allocationId, 1L, 2L, YearWeek.of(2026, 10),
                    BigDecimal.valueOf(20), BigDecimal.valueOf(50),
                    false, null, null, null, null, null, 2L
            );

            // DB entity has version 3
            WeeklyProjectAllocationJpaEntity dbEntity = new WeeklyProjectAllocationJpaEntity(
                    allocationId, 1L, 2L, 2026, 10,
                    BigDecimal.valueOf(25), BigDecimal.valueOf(60),
                    false, null, null, null, null, null, 3L
            );

            when(repository.findById(allocationId)).thenReturn(Optional.of(dbEntity));

            assertThrows(ObjectOptimisticLockingFailureException.class, () -> adapter.delete(domain));
            verify(repository, never()).delete(any());
            verify(repository, never()).deleteById(any());
        }
    }
}
