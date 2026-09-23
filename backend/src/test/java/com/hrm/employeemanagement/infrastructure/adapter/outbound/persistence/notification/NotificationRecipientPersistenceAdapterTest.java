package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationRecipientJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRecipientRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("NotificationRecipientPersistenceAdapter & TransactionalHelper Tests (Concurrency & Data Integrity)")
class NotificationRecipientPersistenceAdapterTest {

    private SpringDataNotificationRecipientRepository repository;
    private TransactionalNotificationRecipientSaveHelper saveHelper;
    private NotificationRecipientPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataNotificationRecipientRepository.class);
        saveHelper = new TransactionalNotificationRecipientSaveHelper(repository);
        adapter = new NotificationRecipientPersistenceAdapter(repository, saveHelper);
    }

    @Test
    @DisplayName("saveAndFlushRequiresNew: Bắt đúng lỗi duplicate key (MySQL 1062 / uk_notification_recipient_event_user) và trả về Optional.empty()")
    void saveHelper_duplicateKey_returnsEmpty() {
        SQLException sqlException = new SQLException("Duplicate entry '1-100' for key 'uk_notification_recipient_event_user'", "23000", 1062);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Constraint violation", sqlException);

        when(repository.saveAndFlush(any())).thenThrow(dive);

        NotificationRecipientJpaEntity entity = new NotificationRecipientJpaEntity(null, 1L, 100L, false, null, false, null, LocalDateTime.now());
        Optional<NotificationRecipientJpaEntity> result = saveHelper.saveAndFlushRequiresNew(entity);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("saveAndFlushRequiresNew: Lỗi Foreign Key vi phạm (không phải duplicate key) -> BẮT BUỘC RETHROW DataIntegrityViolationException")
    void saveHelper_foreignKeyViolation_rethrowsException() {
        SQLException sqlException = new SQLException("Cannot add or update a child row: a foreign key constraint fails (`users`, CONSTRAINT `fk_recipient_user` FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`))", "23000", 1452);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("FK constraint violation", sqlException);

        when(repository.saveAndFlush(any())).thenThrow(dive);

        NotificationRecipientJpaEntity entity = new NotificationRecipientJpaEntity(null, 1L, 99999L, false, null, false, null, LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class, () -> saveHelper.saveAndFlushRequiresNew(entity));
    }

    @Test
    @DisplayName("saveIfAbsent: Khi race condition xảy ra và re-fetch tìm thấy bản ghi -> Trả về item đã tồn tại (idempotent)")
    void saveIfAbsent_raceCondition_returnsExistingItem() {
        TransactionalNotificationRecipientSaveHelper mockHelper = mock(TransactionalNotificationRecipientSaveHelper.class);
        NotificationRecipientPersistenceAdapter adapterWithMock = new NotificationRecipientPersistenceAdapter(repository, mockHelper);

        when(mockHelper.saveAndFlushRequiresNew(any())).thenReturn(Optional.empty());

        NotificationRecipientJpaEntity existingJpa = new NotificationRecipientJpaEntity(
                555L, 1L, 100L, false, null, false, null, LocalDateTime.now()
        );
        when(repository.findByNotificationEventIdAndRecipientUserId(1L, 100L))
                .thenReturn(Optional.of(existingJpa));

        NotificationRecipientItem itemToSave = NotificationRecipientItem.create(
                new NotificationEventId(1L),
                new UserId(100L)
        );

        NotificationRecipientItem result = adapterWithMock.saveIfAbsent(itemToSave);

        assertNotNull(result);
        assertEquals(555L, result.getId().value());
        assertEquals(1L, result.getEventId().value());
        assertEquals(100L, result.getRecipientUserId().value());
    }

    @Test
    @DisplayName("saveIfAbsent: Khi helper trả về empty nhưng re-fetch KHÔNG tìm thấy bản ghi -> Ném IllegalStateException (Ngăn chặn False Success)")
    void saveIfAbsent_raceCondition_refetchFails_throwsIllegalStateException() {
        TransactionalNotificationRecipientSaveHelper mockHelper = mock(TransactionalNotificationRecipientSaveHelper.class);
        NotificationRecipientPersistenceAdapter adapterWithMock = new NotificationRecipientPersistenceAdapter(repository, mockHelper);

        when(mockHelper.saveAndFlushRequiresNew(any())).thenReturn(Optional.empty());
        when(repository.findByNotificationEventIdAndRecipientUserId(1L, 100L)).thenReturn(Optional.empty());

        NotificationRecipientItem itemToSave = NotificationRecipientItem.create(
                new NotificationEventId(1L),
                new UserId(100L)
        );

        assertThrows(IllegalStateException.class, () -> adapterWithMock.saveIfAbsent(itemToSave));
    }
}
