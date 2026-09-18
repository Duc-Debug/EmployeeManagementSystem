package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.CheckTaskDueReminderSentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDueReminderPort;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.notification.TaskDueReminderPolicy;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEventRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataTaskDueReminderNotificationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskDueReminderTaskRepository;

/**
 * Persistence Adapter triển khai LoadTaskDueReminderPort và CheckTaskDueReminderSentPort (QTN-19).
 */
@Component
public class TaskDueReminderPersistenceAdapter implements LoadTaskDueReminderPort, CheckTaskDueReminderSentPort {

    private static final Set<TaskStatus> EXCLUDED_STATUSES = Set.of(TaskStatus.DONE, TaskStatus.CANCELLED);

    private final SpringDataTaskDueReminderTaskRepository taskRepository;
    private final TaskPersistenceMapper taskMapper;
    private final SpringDataNotificationEventRepository eventRepository;
    private final SpringDataTaskDueReminderNotificationRepository notificationRepository;

    public TaskDueReminderPersistenceAdapter(
            SpringDataTaskDueReminderTaskRepository taskRepository,
            TaskPersistenceMapper taskMapper,
            SpringDataNotificationEventRepository eventRepository,
            SpringDataTaskDueReminderNotificationRepository notificationRepository
    ) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.taskMapper = Objects.requireNonNull(taskMapper, "taskMapper must not be null");
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
    }

    @Override
    public List<Task> findTasksDueBetween(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return List.of();
        }
        return taskRepository.findTasksDueBetween(fromDate, toDate, EXCLUDED_STATUSES).stream()
                .map(taskMapper::toDomain)
                .toList();
    }

    @Override
    public List<Task> findUpcomingTasksByAssignee(EmployeeId assigneeId, LocalDate fromDate, LocalDate toDate) {
        if (assigneeId == null || assigneeId.value() == null || fromDate == null || toDate == null) {
            return List.of();
        }
        return taskRepository.findUpcomingTasksByAssignee(assigneeId.value(), fromDate, toDate, EXCLUDED_STATUSES).stream()
                .map(taskMapper::toDomain)
                .toList();
    }

    @Override
    public boolean hasReminderBeenSent(UserId recipientId, Long taskId, LocalDate dueDate) {
        if (recipientId == null || taskId == null || dueDate == null) {
            return false;
        }

        // 1. Kiểm tra trong bảng notification_events qua source_event_key (QTN-19)
        String sourceEventKey = TaskDueReminderPolicy.buildSourceEventKey(taskId, dueDate);
        if (eventRepository.findBySourceEventKey(sourceEventKey).isPresent()) {
            return true;
        }

        // 2. Kiểm tra trong bảng notifications truyền thống (QTN-19)
        return notificationRepository.existsByRecipientIdAndTypeAndTargetId(
                recipientId.value(),
                "TASK_DUE_REMINDER",
                taskId
        );
    }
}
