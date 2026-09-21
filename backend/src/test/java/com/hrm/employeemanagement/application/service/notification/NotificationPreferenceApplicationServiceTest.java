package com.hrm.employeemanagement.application.service.notification;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.notification.NotificationPreferenceResult;
import com.hrm.employeemanagement.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.GetOrCreateNotificationPreferencePort;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryChannel;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.NotificationPreferenceId;
import com.hrm.employeemanagement.domain.user.UserId;

class NotificationPreferenceApplicationServiceTest {

    private LoadNotificationPreferencePort loadNotificationPreferencePort;
    private SaveNotificationPreferencePort saveNotificationPreferencePort;
    private GetOrCreateNotificationPreferencePort getOrCreateNotificationPreferencePort;
    private NotificationPreferenceApplicationService service;

    @BeforeEach
    void setUp() {
        loadNotificationPreferencePort = Mockito.mock(LoadNotificationPreferencePort.class);
        saveNotificationPreferencePort = Mockito.mock(SaveNotificationPreferencePort.class);
        getOrCreateNotificationPreferencePort = Mockito.mock(GetOrCreateNotificationPreferencePort.class);
        service = new NotificationPreferenceApplicationService(
                loadNotificationPreferencePort,
                saveNotificationPreferencePort,
                getOrCreateNotificationPreferencePort
        );
    }

    @Test
    @DisplayName("Lấy cấu hình khi chưa tồn tại trả mặc định mà không ghi DB")
    void shouldCreateDefaultWhenNotFound() {
        Long userIdVal = 101L;
        UserId userId = new UserId(userIdVal);
        when(loadNotificationPreferencePort.findByUserId(userId)).thenReturn(Optional.empty());
        when(getOrCreateNotificationPreferencePort.getOrCreate(userId))
                .thenReturn(NotificationPreference.createDefault(userId));
        NotificationPreferenceResult result = service.getMyPreference(userIdVal);

        assertNotNull(result);
        assertEquals(userIdVal, result.userId());
        assertTrue(result.inAppEnabled());
        assertTrue(result.emailEnabled());
        assertEquals(3, result.taskDueReminderDays());
        verify(getOrCreateNotificationPreferencePort).getOrCreate(userId);
    }

    @Test
    void partialUpdateMustPreserveUnspecifiedFields() {
        Long userIdVal = 101L;
        UserId userId = new UserId(userIdVal);
        NotificationPreference existing = NotificationPreference.createDefault(userId);
        existing.update(
                true, true,
                NotificationDeliveryChannel.EMAIL_ONLY, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationFrequency.IMMEDIATE, 7,
                com.hrm.employeemanagement.domain.notification.QuietHours.of(
                        true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        );
        when(loadNotificationPreferencePort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(saveNotificationPreferencePort.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceResult result = service.updateMyPreference(userIdVal,
                new UpdateNotificationPreferenceCommand(
                        null, null, null, null, null, null, null, null,
                        NotificationFrequency.DAILY_DIGEST, null, null, null, null));

        assertEquals(NotificationFrequency.DAILY_DIGEST, result.frequency());
        assertEquals(NotificationDeliveryChannel.EMAIL_ONLY, result.taskAssignedChannel());
        assertEquals(7, result.taskDueReminderDays());
        assertTrue(result.quietHoursEnabled());
    }

    @Test
    @DisplayName("Cập nhật cấu hình thành công khi dữ liệu hợp lệ")
    void shouldUpdatePreferenceSuccessfully() {
        Long userIdVal = 101L;
        UserId userId = new UserId(userIdVal);
        NotificationPreference existing = NotificationPreference.createDefault(userId);
        existing.setId(new NotificationPreferenceId(1L));

        when(loadNotificationPreferencePort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(saveNotificationPreferencePort.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateNotificationPreferenceCommand command = new UpdateNotificationPreferenceCommand(
                true,
                false,
                NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationFrequency.DAILY_DIGEST,
                5,
                true,
                LocalTime.of(22, 0),
                LocalTime.of(7, 0)
        );

        NotificationPreferenceResult result = service.updateMyPreference(userIdVal, command);

        assertNotNull(result);
        assertFalse(result.emailEnabled());
        assertEquals(5, result.taskDueReminderDays());
        assertEquals(NotificationFrequency.DAILY_DIGEST, result.frequency());
        assertTrue(result.quietHoursEnabled());
        assertEquals(LocalTime.of(22, 0), result.quietHoursStart());
        assertEquals(LocalTime.of(7, 0), result.quietHoursEnd());
    }

    @Test
    @DisplayName("Khôi phục mặc định resetMyPreference thành công")
    void shouldResetPreferenceSuccessfully() {
        Long userIdVal = 101L;
        UserId userId = new UserId(userIdVal);
        NotificationPreference existing = NotificationPreference.createDefault(userId);
        existing.setId(new NotificationPreferenceId(1L));
        existing.update(
                true, false,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationFrequency.WEEKLY_DIGEST,
                7, null
        );

        when(loadNotificationPreferencePort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(saveNotificationPreferencePort.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceResult result = service.resetMyPreference(userIdVal);

        assertNotNull(result);
        assertTrue(result.inAppEnabled());
        assertTrue(result.emailEnabled());
        assertEquals(3, result.taskDueReminderDays());
        assertEquals(NotificationFrequency.IMMEDIATE, result.frequency());
    }
}
