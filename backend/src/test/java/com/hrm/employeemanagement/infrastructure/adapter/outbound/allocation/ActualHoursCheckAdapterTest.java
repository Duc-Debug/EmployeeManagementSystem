package com.hrm.employeemanagement.infrastructure.adapter.outbound.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActualHoursCheckAdapter Unit Tests (Finding 2 Fix)")
class ActualHoursCheckAdapterTest {

    @Mock
    private SpringDataTaskRepository taskRepository;

    private ActualHoursCheckAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ActualHoursCheckAdapter(Optional.of(taskRepository));
    }

    @Test
    @DisplayName("Should return false when employeeId or projectId is null")
    void shouldReturnFalseWhenIdsNull() {
        assertFalse(adapter.hasActualHours(null, 1L, new YearWeek(2026, 38)));
        assertFalse(adapter.hasActualHours(1L, null, new YearWeek(2026, 38)));
    }

    @Test
    @DisplayName("Should return true when task has actualHours and falls exactly within allocation week")
    void shouldReturnTrueWhenTaskInWeek() {
        Long employeeId = 10L;
        Long projectId = 100L;
        YearWeek targetWeek = new YearWeek(2026, 38); // 14/09/2026 to 20/09/2026

        TaskJpaEntity task = new TaskJpaEntity();
        task.setAssigneeId(employeeId);
        task.setActualHours(BigDecimal.valueOf(8.5));
        task.setActualEndDate(LocalDate.of(2026, 9, 16)); // Wednesday in week 38

        when(taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId)).thenReturn(List.of(task));

        boolean result = adapter.hasActualHours(employeeId, projectId, targetWeek);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when task has actualHours but belongs to a different week")
    void shouldReturnFalseWhenTaskInDifferentWeek() {
        Long employeeId = 10L;
        Long projectId = 100L;
        YearWeek targetWeek = new YearWeek(2026, 38); // 14/09/2026 to 20/09/2026

        TaskJpaEntity pastTask = new TaskJpaEntity();
        pastTask.setAssigneeId(employeeId);
        pastTask.setActualHours(BigDecimal.valueOf(20.0));
        pastTask.setActualEndDate(LocalDate.of(2026, 1, 15)); // Week 3 (months earlier)

        when(taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId)).thenReturn(List.of(pastTask));

        boolean result = adapter.hasActualHours(employeeId, projectId, targetWeek);

        assertFalse(result, "Actual hours from Week 3 should not block removing allocation in Week 38");
    }

    @Test
    @DisplayName("Should return false when task has 0 actual hours")
    void shouldReturnFalseWhenActualHoursZero() {
        Long employeeId = 10L;
        Long projectId = 100L;
        YearWeek targetWeek = new YearWeek(2026, 38);

        TaskJpaEntity task = new TaskJpaEntity();
        task.setAssigneeId(employeeId);
        task.setActualHours(BigDecimal.ZERO);
        task.setActualEndDate(LocalDate.of(2026, 9, 16));

        when(taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId)).thenReturn(List.of(task));

        boolean result = adapter.hasActualHours(employeeId, projectId, targetWeek);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when task belongs to another employee")
    void shouldReturnFalseWhenAssignedToAnotherEmployee() {
        Long employeeId = 10L;
        Long otherEmployeeId = 20L;
        Long projectId = 100L;
        YearWeek targetWeek = new YearWeek(2026, 38);

        TaskJpaEntity task = new TaskJpaEntity();
        task.setAssigneeId(otherEmployeeId);
        task.setActualHours(BigDecimal.valueOf(15));
        task.setActualEndDate(LocalDate.of(2026, 9, 16));

        when(taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId)).thenReturn(List.of(task));

        boolean result = adapter.hasActualHours(employeeId, projectId, targetWeek);

        assertFalse(result);
    }
}
