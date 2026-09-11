package com.hrm.employeemanagement.application.service.task.cascade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.task.cascade.AffectedMilestoneResult;
import com.hrm.employeemanagement.application.dto.task.cascade.AffectedTaskResult;
import com.hrm.employeemanagement.application.dto.task.cascade.CascadeDelayWarningResult;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneId;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependencyType;
import com.hrm.employeemanagement.domain.user.UserId;

class CascadeDelayEvaluatorTest {

    private CascadeDelayEvaluator evaluator;
    private final ProjectId projectId = new ProjectId(1L);
    private final UserId userId = new UserId(100L);

    @BeforeEach
    void setUp() {
        evaluator = new CascadeDelayEvaluator();
    }

    private Task createTask(Long id, String code, String name, LocalDate startDate, LocalDate dueDate, Integer slackDays) {
        return new Task(
                new TaskId(id),
                projectId,
                null,
                code,
                name,
                "Description",
                TaskType.TASK,
                new EmployeeId(50L),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10),
                TaskStatus.IN_PROGRESS,
                1,
                startDate,
                dueDate,
                null,
                slackDays != null ? slackDays : 0,
                userId,
                null,
                null,
                0L
        );
    }

    private TaskDependency createDependency(Long id, Long predId, Long succId, Integer lagDays) {
        return new TaskDependency(
                id,
                projectId,
                new TaskId(predId),
                new TaskId(succId),
                TaskDependencyType.FINISH_TO_START,
                lagDays != null ? lagDays : 0,
                userId,
                null
        );
    }

    @Test
    @DisplayName("Multiple predecessors: Task D receives maximum delay (8 days) from merging branch C instead of shorter branch B (3 days)")
    void evaluate_MultiplePredecessors_TakesMaximumDelay() {
        // Setup DAG:
        // A (+8 days delay)
        // A -> B (slack 5) => B net delay = 8 - 5 = 3
        // A -> C (slack 0) => C net delay = 8 - 0 = 8
        // B -> D (slack 0)
        // C -> D (slack 0)
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate dueDate = LocalDate.of(2026, 10, 5);

        Task taskA = createTask(10L, "TASK-A", "Task A (Root)", startDate, dueDate, 0);
        Task taskB = createTask(20L, "TASK-B", "Task B", startDate, dueDate, 5);
        Task taskC = createTask(30L, "TASK-C", "Task C", startDate, dueDate, 0);
        Task taskD = createTask(40L, "TASK-D", "Task D", startDate, dueDate, 0);

        TaskDependency depAB = createDependency(101L, 10L, 20L, 0);
        TaskDependency depAC = createDependency(102L, 10L, 30L, 0);
        TaskDependency depBD = createDependency(103L, 20L, 40L, 0);
        TaskDependency depCD = createDependency(104L, 30L, 40L, 0);

        Milestone milestoneM = new Milestone(
                new MilestoneId(500L),
                projectId,
                "Milestone Release",
                "Description",
                LocalDate.of(2026, 10, 25),
                null,
                Set.of(new TaskId(40L)),
                userId,
                null,
                null,
                0L
        );

        LocalDate newActualEndDate = dueDate.plusDays(8); // A slips by 8 days

        CascadeDelayWarningResult result = evaluator.evaluate(
                taskA,
                newActualEndDate,
                List.of(taskA, taskB, taskC, taskD),
                List.of(depAB, depAC, depBD, depCD),
                List.of(milestoneM)
        );

        assertNotNull(result);
        assertEquals(8L, result.slipDays());
        assertFalse(result.chainOnTimeDueToSlack());
        assertEquals(3, result.affectedTasks().size());

        AffectedTaskResult resB = result.affectedTasks().stream().filter(t -> t.taskId().equals(20L)).findFirst().orElseThrow();
        assertEquals(3L, resB.delayDays());

        AffectedTaskResult resC = result.affectedTasks().stream().filter(t -> t.taskId().equals(30L)).findFirst().orElseThrow();
        assertEquals(8L, resC.delayDays());

        AffectedTaskResult resD = result.affectedTasks().stream().filter(t -> t.taskId().equals(40L)).findFirst().orElseThrow();
        assertEquals(8L, resD.delayDays(), "Task D must receive maximum delay across all predecessor paths (8 days)");

        assertEquals(1, result.affectedMilestones().size());
        AffectedMilestoneResult resM = result.affectedMilestones().get(0);
        assertEquals(8L, resM.delayDays());
    }

    @Test
    @DisplayName("Long chain delay propagation: A -> B -> C -> D with intermediate slack absorption")
    void evaluate_LongChain_PropagatesAndAbsorbsSlack() {
        // A (+5 days delay) -> B (slack 0) => 5 -> C (slack 2) => 3 -> D (slack 1) => 2
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate dueDate = LocalDate.of(2026, 10, 5);

        Task taskA = createTask(10L, "TASK-A", "Task A", startDate, dueDate, 0);
        Task taskB = createTask(20L, "TASK-B", "Task B", startDate, dueDate, 0);
        Task taskC = createTask(30L, "TASK-C", "Task C", startDate, dueDate, 2);
        Task taskD = createTask(40L, "TASK-D", "Task D", startDate, dueDate, 1);

        TaskDependency depAB = createDependency(101L, 10L, 20L, 0);
        TaskDependency depBC = createDependency(102L, 20L, 30L, 0);
        TaskDependency depCD = createDependency(103L, 30L, 40L, 0);

        LocalDate newActualEndDate = dueDate.plusDays(5);

        CascadeDelayWarningResult result = evaluator.evaluate(
                taskA,
                newActualEndDate,
                List.of(taskA, taskB, taskC, taskD),
                List.of(depAB, depBC, depCD),
                List.of()
        );

        assertEquals(5L, result.slipDays());
        assertEquals(3, result.affectedTasks().size());

        AffectedTaskResult resB = result.affectedTasks().stream().filter(t -> t.taskId().equals(20L)).findFirst().orElseThrow();
        assertEquals(5L, resB.delayDays());

        AffectedTaskResult resC = result.affectedTasks().stream().filter(t -> t.taskId().equals(30L)).findFirst().orElseThrow();
        assertEquals(3L, resC.delayDays());

        AffectedTaskResult resD = result.affectedTasks().stream().filter(t -> t.taskId().equals(40L)).findFirst().orElseThrow();
        assertEquals(2L, resD.delayDays());
    }

    @Test
    @DisplayName("Intermediate slack fully protects downstream tasks")
    void evaluate_IntermediateSlackFullyProtects() {
        // A (+3 days delay) -> B (slack 5) => protected (0 delay) -> C (slack 0) => protected (0 delay)
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate dueDate = LocalDate.of(2026, 10, 5);

        Task taskA = createTask(10L, "TASK-A", "Task A", startDate, dueDate, 0);
        Task taskB = createTask(20L, "TASK-B", "Task B", startDate, dueDate, 5);
        Task taskC = createTask(30L, "TASK-C", "Task C", startDate, dueDate, 0);

        TaskDependency depAB = createDependency(101L, 10L, 20L, 0);
        TaskDependency depBC = createDependency(102L, 20L, 30L, 0);

        LocalDate newActualEndDate = dueDate.plusDays(3);

        CascadeDelayWarningResult result = evaluator.evaluate(
                taskA,
                newActualEndDate,
                List.of(taskA, taskB, taskC),
                List.of(depAB, depBC),
                List.of()
        );

        assertTrue(result.chainOnTimeDueToSlack());
        assertEquals(2, result.affectedTasks().size());

        for (AffectedTaskResult t : result.affectedTasks()) {
            assertEquals(0L, t.delayDays());
            assertTrue(t.protectedBySlack());
        }
    }
}
