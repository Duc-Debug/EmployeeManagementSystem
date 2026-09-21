package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.domain.notification.TaskDueReminderPolicy;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEventJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationRecipientJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEventRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRecipientRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataTaskDueReminderNotificationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskDueReminderTaskRepository;

class TaskDueReminderPersistenceAdapterTest {

    private SpringDataTaskDueReminderTaskRepository taskRepository;
    private TaskPersistenceMapper taskMapper;
    private SpringDataNotificationEventRepository eventRepository;
    private SpringDataNotificationRecipientRepository recipientRepository;
    private SpringDataTaskDueReminderNotificationRepository notificationRepository;
    private TaskDueReminderPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        taskRepository = mock(SpringDataTaskDueReminderTaskRepository.class);
        taskMapper = mock(TaskPersistenceMapper.class);
        eventRepository = mock(SpringDataNotificationEventRepository.class);
        recipientRepository = mock(SpringDataNotificationRecipientRepository.class);
        notificationRepository = mock(SpringDataTaskDueReminderNotificationRepository.class);

        adapter = new TaskDueReminderPersistenceAdapter(
                taskRepository,
                taskMapper,
                eventRepository,
                recipientRepository,
                notificationRepository
        );
    }

    @Test
    @DisplayName("QTN-19 Atomic Proof-of-Delivery: Event tồn tại nhưng recipient bị rollback -> Trả về false để cho phép retry")
    void hasReminderBeenSent_eventExistsWithoutRecipient_returnsFalse() {
        UserId recipientId = new UserId(42L);
        Long taskId = 100L;
        LocalDate dueDate = LocalDate.of(2026, 9, 20);

        String key = TaskDueReminderPolicy.buildSourceEventKey(taskId, recipientId.value(), dueDate);
        NotificationEventJpaEntity event = new NotificationEventJpaEntity();
        event.setId(888L);

        // Giả lập: REQUIRES_NEW đã commit event header thành công
        when(eventRepository.findBySourceEventKey(key)).thenReturn(Optional.of(event));
        // Nhưng transaction ngoài rollback khiến recipient record không tồn tại trong DB
        when(recipientRepository.findByNotificationEventIdAndRecipientUserId(888L, 42L)).thenReturn(Optional.empty());
        // Và bảng notifications cũng không có
        when(notificationRepository.existsByRecipientIdAndTypeAndTargetIdAndContentContaining(42L, "TASK_DUE_REMINDER", taskId, dueDate.toString())).thenReturn(false);

        boolean sent = adapter.hasReminderBeenSent(recipientId, taskId, dueDate);

        // Kết quả: Hệ thống KHÔNG coi là đã gửi -> cho phép retry gửi bù thông báo!
        assertFalse(sent, "Phải trả về false khi recipient chưa được tạo để hệ thống gửi bù");
    }

    @Test
    @DisplayName("QTN-19: Cả Event và Recipient đều tồn tại hợp lệ -> Trả về true")
    void hasReminderBeenSent_eventAndRecipientBothExist_returnsTrue() {
        UserId recipientId = new UserId(42L);
        Long taskId = 100L;
        LocalDate dueDate = LocalDate.of(2026, 9, 20);

        String key = TaskDueReminderPolicy.buildSourceEventKey(taskId, recipientId.value(), dueDate);
        NotificationEventJpaEntity event = new NotificationEventJpaEntity();
        event.setId(888L);

        when(eventRepository.findBySourceEventKey(key)).thenReturn(Optional.of(event));
        when(recipientRepository.findByNotificationEventIdAndRecipientUserId(888L, 42L))
                .thenReturn(Optional.of(new NotificationRecipientJpaEntity()));

        boolean sent = adapter.hasReminderBeenSent(recipientId, taskId, dueDate);

        assertTrue(sent, "Phải trả về true khi cả event và recipient đều tồn tại");
    }

    @Test
    @DisplayName("QTN-19: Tồn tại trong bảng notifications truyền thống đúng dueDate -> Trả về true")
    void hasReminderBeenSent_legacyNotificationExists_returnsTrue() {
        UserId recipientId = new UserId(42L);
        Long taskId = 100L;
        LocalDate dueDate = LocalDate.of(2026, 9, 20);

        when(eventRepository.findBySourceEventKey(anyString())).thenReturn(Optional.empty());
        when(notificationRepository.existsByRecipientIdAndTypeAndTargetIdAndContentContaining(42L, "TASK_DUE_REMINDER", taskId, dueDate.toString())).thenReturn(true);

        boolean sent = adapter.hasReminderBeenSent(recipientId, taskId, dueDate);

        assertTrue(sent, "Phải trả về true khi bảng notifications truyền thống có bản ghi đúng dueDate");
    }

    @Test
    @DisplayName("QTN-19: Bảng notifications truyền thống chỉ có hạn cũ (20/09), hạn mới (25/09) chưa có -> Trả về false")
    void hasReminderBeenSent_legacyNotificationExistsForDifferentDueDate_returnsFalse() {
        UserId recipientId = new UserId(42L);
        Long taskId = 100L;
        LocalDate oldDueDate = LocalDate.of(2026, 9, 20);
        LocalDate newDueDate = LocalDate.of(2026, 9, 25);

        when(eventRepository.findBySourceEventKey(anyString())).thenReturn(Optional.empty());
        // Giả lập: Bản ghi chỉ có cho hạn cũ 20/09, với hạn mới 25/09 repository trả về false
        when(notificationRepository.existsByRecipientIdAndTypeAndTargetIdAndContentContaining(42L, "TASK_DUE_REMINDER", taskId, newDueDate.toString())).thenReturn(false);

        boolean sent = adapter.hasReminderBeenSent(recipientId, taskId, newDueDate);

        assertFalse(sent, "Phải trả về false khi deadline thay đổi để hệ thống gửi reminder cho deadline mới");
    }

    @Test
    @DisplayName("QTN-19: Cả 2 bảng đều không có thông báo -> Trả về false")
    void hasReminderBeenSent_neitherExists_returnsFalse() {
        UserId recipientId = new UserId(42L);
        Long taskId = 100L;
        LocalDate dueDate = LocalDate.of(2026, 9, 20);

        when(eventRepository.findBySourceEventKey(anyString())).thenReturn(Optional.empty());
        when(notificationRepository.existsByRecipientIdAndTypeAndTargetIdAndContentContaining(anyLong(), anyString(), anyLong(), anyString())).thenReturn(false);

        boolean sent = adapter.hasReminderBeenSent(recipientId, taskId, dueDate);

        assertFalse(sent, "Phải trả về false khi chưa từng gửi thông báo");
    }

    @Test
    @DisplayName("QTN-19: Tham số null -> Trả về false an toàn")
    void hasReminderBeenSent_nullParameters_returnsFalse() {
        assertFalse(adapter.hasReminderBeenSent(null, 1L, LocalDate.now()));
        assertFalse(adapter.hasReminderBeenSent(new UserId(1L), null, LocalDate.now()));
        assertFalse(adapter.hasReminderBeenSent(new UserId(1L), 1L, null));
    }
}