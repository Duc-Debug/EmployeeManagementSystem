package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;

class TaskPersistenceMapperTest {

    private final TaskPersistenceMapper mapper = new TaskPersistenceMapper();

    @Test
    void shouldPreserveAllSchedulingFieldsDuringRoundTrip() {
        LocalDate plannedStart = LocalDate.of(2026, 9, 1);
        LocalDate plannedEnd = LocalDate.of(2026, 9, 10);
        LocalDate start = LocalDate.of(2026, 9, 2);
        LocalDate due = LocalDate.of(2026, 9, 12);
        LocalDate actualEnd = LocalDate.of(2026, 9, 11);

        TaskJpaEntity source = new TaskJpaEntity(
                1L, 2L, null, "TASK-1", "Preserve schedule", null,
                TaskType.TASK, 3L, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN,
                TaskStatus.DONE, 1, plannedStart, plannedEnd, start, due, actualEnd, 3,
                4L, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 9, 11, 17, 0), 5L);

        Task domain = mapper.toDomain(source);
        TaskJpaEntity roundTripped = mapper.toJpaEntity(domain);

        assertThat(roundTripped.getPlannedStartDate()).isEqualTo(plannedStart);
        assertThat(roundTripped.getPlannedEndDate()).isEqualTo(plannedEnd);
        assertThat(roundTripped.getStartDate()).isEqualTo(start);
        assertThat(roundTripped.getDueDate()).isEqualTo(due);
        assertThat(roundTripped.getActualEndDate()).isEqualTo(actualEnd);
        assertThat(roundTripped.getSlackDays()).isEqualTo(3);
    }
}
