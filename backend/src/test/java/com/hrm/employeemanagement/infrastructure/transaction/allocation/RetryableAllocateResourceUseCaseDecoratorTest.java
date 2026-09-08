package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetryableAllocateResourceUseCaseDecorator Unit Tests")
class RetryableAllocateResourceUseCaseDecoratorTest {

    @Mock
    private AllocateResourceUseCase transactionalDelegate;

    private RetryableAllocateResourceUseCaseDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new RetryableAllocateResourceUseCaseDecorator(transactionalDelegate, 3);
    }

    @Test
    @DisplayName("Gặp DataIntegrityViolationException lần 1 -> Retry ở lần 2 thành công")
    void testAllocateResource_RetrySuccessOnSecondAttempt() {
        AllocateResourceCommand command = new AllocateResourceCommand(100L, 10L, 2026, 36, BigDecimal.valueOf(20));
        WeeklyCapacityResult expectedResult = new WeeklyCapacityResult(
                100L, "EMP001", "Nguyễn Văn A", 2026, 36, 40,
                BigDecimal.valueOf(40), BigDecimal.valueOf(20), BigDecimal.valueOf(20), false, null
        );

        when(transactionalDelegate.allocateResource(command))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"))
                .thenReturn(expectedResult);

        WeeklyCapacityResult result = decorator.allocateResource(command);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(transactionalDelegate, times(2)).allocateResource(command);
    }

    @Test
    @DisplayName("Vượt quá số lần retry tối đa -> Ném ra DataIntegrityViolationException")
    void testAllocateResource_ExceedsMaxRetries_ThrowsException() {
        AllocateResourceCommand command = new AllocateResourceCommand(100L, 10L, 2026, 36, BigDecimal.valueOf(20));

        when(transactionalDelegate.allocateResource(command))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> decorator.allocateResource(command)
        );

        verify(transactionalDelegate, times(3)).allocateResource(command);
    }

    @Test
    @DisplayName("getWeeklyCapacities chuyển giao trực tiếp cho delegate")
    void testGetWeeklyCapacities_DelegatesDirectly() {
        List<Long> empIds = List.of(100L);
        when(transactionalDelegate.getWeeklyCapacities(empIds, 2026, 36)).thenReturn(List.of());

        List<WeeklyCapacityResult> results = decorator.getWeeklyCapacities(empIds, 2026, 36);

        assertNotNull(results);
        verify(transactionalDelegate, times(1)).getWeeklyCapacities(empIds, 2026, 36);
    }
}
