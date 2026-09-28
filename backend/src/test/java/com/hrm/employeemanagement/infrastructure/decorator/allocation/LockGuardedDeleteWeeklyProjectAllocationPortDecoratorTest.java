package com.hrm.employeemanagement.infrastructure.decorator.allocation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.DeleteWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;

@DisplayName("LockGuardedDeleteWeeklyProjectAllocationPortDecorator Tests (QTN-18 / Hard Delete Guard)")
class LockGuardedDeleteWeeklyProjectAllocationPortDecoratorTest {

    private DeleteWeeklyProjectAllocationPort delegate;
    private CheckAllocationPeriodLockUseCase checkLockUseCase;
    private SpringDataWeeklyProjectAllocationRepository repository;
    private LockGuardedDeleteWeeklyProjectAllocationPortDecorator decorator;

    @BeforeEach
    void setUp() {
        delegate = mock(DeleteWeeklyProjectAllocationPort.class);
        checkLockUseCase = mock(CheckAllocationPeriodLockUseCase.class);
        repository = mock(SpringDataWeeklyProjectAllocationRepository.class);
        decorator = new LockGuardedDeleteWeeklyProjectAllocationPortDecorator(delegate, checkLockUseCase, repository);
    }

    @Test
    @DisplayName("TC-01: Xóa entity phân bổ trong tuần mở -> Cho phép xóa thành công")
    void shouldAllowDeletingAllocationInUnlockedWeek() {
        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                100L, 1L, 10L, YearWeek.of(2026, 15), BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
        );
        doNothing().when(checkLockUseCase).validateWeekNotLocked(2026, 15);

        decorator.delete(alloc);

        verify(checkLockUseCase, times(1)).validateWeekNotLocked(2026, 15);
        verify(delegate, times(1)).delete(alloc);
    }

    @Test
    @DisplayName("TC-02: Xóa entity phân bổ trong tuần đã khóa -> Chặn và ném AllocationPeriodLockedException")
    void shouldBlockDeletingAllocationInLockedWeek() {
        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                100L, 1L, 10L, YearWeek.of(2026, 10), BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
        );
        doThrow(new AllocationPeriodLockedException("Kỳ Quý 1/2026", 2026, 10))
                .when(checkLockUseCase).validateWeekNotLocked(2026, 10);

        assertThatThrownBy(() -> decorator.delete(alloc))
                .isInstanceOf(AllocationPeriodLockedException.class)
                .hasMessageContaining("Kỳ Quý 1/2026");

        verify(delegate, never()).delete(any());
    }

    @Test
    @DisplayName("TC-03: Xóa theo ID trong tuần mở -> Cho phép xóa thành công")
    void shouldAllowDeletingByIdInUnlockedWeek() {
        WeeklyProjectAllocationJpaEntity entity = new WeeklyProjectAllocationJpaEntity(
                200L, 1L, 10L, 2026, 15, BigDecimal.valueOf(20), BigDecimal.valueOf(50), false, null, null, null, 0L
        );
        when(repository.findById(200L)).thenReturn(Optional.of(entity));
        doNothing().when(checkLockUseCase).validateWeekNotLocked(2026, 15);

        decorator.deleteById(200L);

        verify(checkLockUseCase, times(1)).validateWeekNotLocked(2026, 15);
        verify(delegate, times(1)).deleteById(200L);
    }

    @Test
    @DisplayName("TC-04: Xóa theo ID trong tuần đã khóa -> Chặn và ném AllocationPeriodLockedException")
    void shouldBlockDeletingByIdInLockedWeek() {
        WeeklyProjectAllocationJpaEntity entity = new WeeklyProjectAllocationJpaEntity(
                200L, 1L, 10L, 2026, 10, BigDecimal.valueOf(20), BigDecimal.valueOf(50), false, null, null, null, 0L
        );
        when(repository.findById(200L)).thenReturn(Optional.of(entity));
        doThrow(new AllocationPeriodLockedException("Kỳ Quý 1/2026", 2026, 10))
                .when(checkLockUseCase).validateWeekNotLocked(2026, 10);

        assertThatThrownBy(() -> decorator.deleteById(200L))
                .isInstanceOf(AllocationPeriodLockedException.class)
                .hasMessageContaining("Kỳ Quý 1/2026");

        verify(delegate, never()).deleteById(any());
    }
}
