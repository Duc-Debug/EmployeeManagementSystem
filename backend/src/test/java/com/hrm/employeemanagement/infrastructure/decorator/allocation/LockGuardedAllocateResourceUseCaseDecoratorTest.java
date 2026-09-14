package com.hrm.employeemanagement.infrastructure.decorator.allocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LockGuardedAllocateResourceUseCaseDecorator Tests (QTN-18 / TC-02)")
class LockGuardedAllocateResourceUseCaseDecoratorTest {

    @Mock
    private AllocateResourceUseCase delegate;

    @Mock
    private CheckAllocationPeriodLockUseCase checkLockUseCase;

    private LockGuardedAllocateResourceUseCaseDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new LockGuardedAllocateResourceUseCaseDecorator(delegate, checkLockUseCase);
    }

    @Test
    @DisplayName("NCL-06-CN-009-TC-02 (Sai trạng thái - QTN-18): Kỳ đã khóa, chặn sửa phân bổ và yêu cầu mở lại kỳ trước khi sửa")
    void tc02_shouldInterceptAndBlockAllocationWhenPeriodIsLocked() {
        // Given: Tuần 10/2026 nằm trong một kỳ đã bị khóa
        AllocateResourceCommand command = new AllocateResourceCommand(
                10L, 20L, 2026, 10, BigDecimal.valueOf(30.0), null, null
        );

        doThrow(new AllocationPeriodLockedException("Kế hoạch Quý 1/2026", 2026, 10))
                .when(checkLockUseCase).validateWeekNotLocked(2026, 10);

        // When & Then: Bị chặn lại với AllocationPeriodLockedException
        AllocationPeriodLockedException ex = assertThrows(
                AllocationPeriodLockedException.class,
                () -> decorator.allocateResource(command)
        );

        assertEquals("Kế hoạch Quý 1/2026", ex.getPeriodName());
        assertEquals(2026, ex.getYear());
        assertEquals(10, ex.getWeekNumber());

        // Verify: Service gốc của người khác hoàn toàn KHÔNG được gọi
        verify(delegate, never()).allocateResource(any());
    }

    @Test
    @DisplayName("Cho phép phân bổ bình thường khi kỳ chưa bị khóa")
    void shouldProceedWhenPeriodNotLocked() {
        // Given: Tuần 10/2026 không bị khóa
        AllocateResourceCommand command = new AllocateResourceCommand(
                10L, 20L, 2026, 10, BigDecimal.valueOf(30.0), null, null
        );

        doNothing().when(checkLockUseCase).validateWeekNotLocked(2026, 10);

        WeeklyCapacityResult mockResult = new WeeklyCapacityResult(
                10L, "EMP001", "Nguyễn Văn A", 2026, 10, 40,
                BigDecimal.valueOf(40.0), BigDecimal.valueOf(30.0), BigDecimal.valueOf(10.0),
                false, null
        );
        when(delegate.allocateResource(command)).thenReturn(mockResult);

        // When
        WeeklyCapacityResult result = decorator.allocateResource(command);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.employeeId());
        verify(delegate).allocateResource(command);
    }
}
