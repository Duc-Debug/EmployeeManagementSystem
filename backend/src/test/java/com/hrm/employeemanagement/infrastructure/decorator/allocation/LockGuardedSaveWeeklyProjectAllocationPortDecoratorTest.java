package com.hrm.employeemanagement.infrastructure.decorator.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException;

@DisplayName("LockGuardedSaveWeeklyProjectAllocationPortDecorator Tests (QTN-18 / Persistence Mutation Guard)")
class LockGuardedSaveWeeklyProjectAllocationPortDecoratorTest {

    private SaveWeeklyProjectAllocationPort delegate;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private CheckAllocationPeriodLockUseCase checkLockUseCase;
    private LockGuardedSaveWeeklyProjectAllocationPortDecorator decorator;

    @BeforeEach
    void setUp() {
        delegate = mock(SaveWeeklyProjectAllocationPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        checkLockUseCase = mock(CheckAllocationPeriodLockUseCase.class);
        decorator = new LockGuardedSaveWeeklyProjectAllocationPortDecorator(delegate, loadAllocationPort, checkLockUseCase);
    }

    @Test
    @DisplayName("TC-01: Tạo mới phân bổ (id == null) vào tuần mở -> Cho phép lưu thành công")
    void shouldAllowSavingNewAllocationInUnlockedWeek() {
        WeeklyProjectAllocation newAlloc = WeeklyProjectAllocation.createNew(1L, 10L, YearWeek.of(2026, 15), BigDecimal.valueOf(20));
        doNothing().when(checkLockUseCase).validateWeekNotLocked(2026, 15);
        when(delegate.save(newAlloc)).thenReturn(newAlloc);

        WeeklyProjectAllocation result = decorator.save(newAlloc);

        assertThat(result).isNotNull();
        verify(checkLockUseCase, times(1)).validateWeekNotLocked(2026, 15);
        verify(delegate, times(1)).save(newAlloc);
    }

    @Test
    @DisplayName("TC-02: Tạo mới phân bổ vào tuần thuộc kỳ đã khóa -> Ném AllocationPeriodLockedException")
    void shouldBlockSavingNewAllocationInLockedWeek() {
        WeeklyProjectAllocation newAlloc = WeeklyProjectAllocation.createNew(1L, 10L, YearWeek.of(2026, 10), BigDecimal.valueOf(20));
        doThrow(new AllocationPeriodLockedException("Kế hoạch Quý 1/2026", 2026, 10))
                .when(checkLockUseCase).validateWeekNotLocked(2026, 10);

        assertThatThrownBy(() -> decorator.save(newAlloc))
                .isInstanceOf(AllocationPeriodLockedException.class)
                .hasMessageContaining("Kế hoạch Quý 1/2026");

        verify(delegate, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Sửa đổi hoặc gỡ phân bổ (cập nhật số giờ về 0h) trong tuần đã khóa -> Chặn và ném ngoại lệ")
    void shouldBlockDeallocatingOrModifyingAllocationInLockedWeek() {
        YearWeek yw = YearWeek.of(2026, 5);
        WeeklyProjectAllocation existing = new WeeklyProjectAllocation(
                100L, 1L, 10L, yw, BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
        );
        WeeklyProjectAllocation deallocating = new WeeklyProjectAllocation(
                100L, 1L, 10L, yw, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );

        when(loadAllocationPort.loadAllocation(eq(1L), eq(10L), eq(yw))).thenReturn(Optional.of(existing));
        doThrow(new AllocationPeriodLockedException("Kế hoạch Quý 1/2026", 2026, 5))
                .when(checkLockUseCase).validateWeekNotLocked(2026, 5);

        assertThatThrownBy(() -> decorator.save(deallocating))
                .isInstanceOf(AllocationPeriodLockedException.class)
                .hasMessageContaining("Kế hoạch Quý 1/2026");

        verify(delegate, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Cập nhật cờ hệ thống mà không thay đổi giờ/tỷ lệ phân bổ -> Không kích hoạt kiểm tra khóa")
    void shouldAllowSystemStateUpdateWithoutTriggeringLockCheck() {
        YearWeek yw = YearWeek.of(2026, 5);
        WeeklyProjectAllocation existing = new WeeklyProjectAllocation(
                100L, 1L, 10L, yw, BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
        );
        WeeklyProjectAllocation systemUpdated = new WeeklyProjectAllocation(
                100L, 1L, 10L, yw, BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
        );
        systemUpdated.markOverloaded("Quá tải do nghỉ phép", 99L, java.time.LocalDateTime.now());

        when(loadAllocationPort.loadAllocation(eq(1L), eq(10L), eq(yw))).thenReturn(Optional.of(existing));
        when(delegate.save(systemUpdated)).thenReturn(systemUpdated);

        WeeklyProjectAllocation result = decorator.save(systemUpdated);

        assertThat(result).isNotNull();
        verify(checkLockUseCase, never()).validateWeekNotLocked(any(Integer.class), any(Integer.class));
        verify(delegate, times(1)).save(systemUpdated);
    }
}
