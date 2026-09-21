package com.hrm.employeemanagement.domain.notification;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.notification.NotificationPreferenceValidationException;
import com.hrm.employeemanagement.domain.user.UserId;

class NotificationPreferenceTest {

    @Test
    @DisplayName("Tạo cấu hình mặc định phải đầy đủ các trường hợp lệ và bật cả 2 kênh")
    void shouldCreateDefaultPreferenceSuccessfully() {
        UserId userId = new UserId(100L);
        NotificationPreference pref = NotificationPreference.createDefault(userId);

        assertNotNull(pref);
        assertEquals(userId, pref.getUserId());
        assertTrue(pref.isInAppEnabled());
        assertTrue(pref.isEmailEnabled());
        assertEquals(NotificationDeliveryChannel.ALL, pref.getTaskAssignedChannel());
        assertEquals(NotificationDeliveryChannel.ALL, pref.getTaskDueReminderChannel());
        assertEquals(NotificationDeliveryChannel.IN_APP_ONLY, pref.getTaskCommentChannel());
        assertEquals(NotificationDeliveryChannel.ALL, pref.getTimesheetReminderChannel());
        assertEquals(NotificationDeliveryChannel.ALL, pref.getAllocationChangedChannel());
        assertEquals(NotificationDeliveryChannel.ALL, pref.getScheduleConflictChannel());
        assertEquals(NotificationFrequency.IMMEDIATE, pref.getFrequency());
        assertEquals(3, pref.getTaskDueReminderDays());
        assertFalse(pref.getQuietHours().enabled());
    }

    @Test
    @DisplayName("Cập nhật hợp lệ phải thay đổi các giá trị thành công")
    void shouldUpdatePreferencesSuccessfully() {
        NotificationPreference pref = NotificationPreference.createDefault(new UserId(100L));

        pref.update(
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
                QuietHours.of(true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        );

        assertTrue(pref.isInAppEnabled());
        assertFalse(pref.isEmailEnabled());
        assertEquals(NotificationFrequency.DAILY_DIGEST, pref.getFrequency());
        assertEquals(5, pref.getTaskDueReminderDays());
        assertTrue(pref.getQuietHours().enabled());
        assertEquals(LocalTime.of(22, 0), pref.getQuietHours().startTime());
        assertEquals(LocalTime.of(7, 0), pref.getQuietHours().endTime());
    }

    @Test
    @DisplayName("Vi phạm BR-03: Tắt cả 2 kênh cho cảnh báo xung đột lịch phải ném ngoại lệ")
    void shouldThrowExceptionWhenDisablingAllChannelsForScheduleConflict() {
        NotificationPreference pref = NotificationPreference.createDefault(new UserId(100L));

        assertThrows(NotificationPreferenceValidationException.class, () ->
                pref.update(
                        true,
                        true,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.NONE, // Chặn NONE ở đây
                        NotificationFrequency.IMMEDIATE,
                        3,
                        QuietHours.disabled()
                )
        );
    }

    @Test
    @DisplayName("Vi phạm BR-03: Tắt cả 2 kênh cho cảnh báo thay đổi phân bổ phải ném ngoại lệ")
    void shouldThrowExceptionWhenDisablingAllChannelsForAllocationChanged() {
        NotificationPreference pref = NotificationPreference.createDefault(new UserId(100L));

        assertThrows(NotificationPreferenceValidationException.class, () ->
                pref.update(
                        true,
                        true,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.NONE, // Chặn NONE ở đây
                        NotificationDeliveryChannel.ALL,
                        NotificationFrequency.IMMEDIATE,
                        3,
                        QuietHours.disabled()
                )
        );
    }

    @Test
    @DisplayName("Số ngày nhắc việc ngoài khoảng [1..14] phải ném ngoại lệ")
    void shouldThrowExceptionWhenReminderDaysOutOfRange() {
        NotificationPreference pref = NotificationPreference.createDefault(new UserId(100L));

        assertThrows(NotificationPreferenceValidationException.class, () ->
                pref.update(
                        true, true,
                        NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                        NotificationFrequency.IMMEDIATE,
                        0, // Nhỏ hơn 1
                        QuietHours.disabled()
                )
        );

        assertThrows(NotificationPreferenceValidationException.class, () ->
                pref.update(
                        true, true,
                        NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                        NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                        NotificationFrequency.IMMEDIATE,
                        15, // Lớn hơn 14
                        QuietHours.disabled()
                )
        );
    }

    @Test
    @DisplayName("Khôi phục mặc định resetToDefault phải đưa mọi trường về ban đầu")
    void shouldResetToDefaultSuccessfully() {
        NotificationPreference pref = NotificationPreference.createDefault(new UserId(100L));
        pref.update(
                true, false,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.NONE, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationFrequency.WEEKLY_DIGEST,
                7,
                QuietHours.of(true, LocalTime.of(23, 0), LocalTime.of(6, 0))
        );

        pref.resetToDefault();

        assertTrue(pref.isInAppEnabled());
        assertTrue(pref.isEmailEnabled());
        assertEquals(NotificationDeliveryChannel.ALL, pref.getTaskAssignedChannel());
        assertEquals(NotificationFrequency.IMMEDIATE, pref.getFrequency());
        assertEquals(3, pref.getTaskDueReminderDays());
        assertFalse(pref.getQuietHours().enabled());
    }

    @Test
    void shouldRejectCriticalChannelsDisabledByMasterToggles() {
        NotificationPreference pref = NotificationPreference.createDefault(new UserId(100L));

        assertThrows(NotificationPreferenceValidationException.class, () -> pref.update(
                false, false,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationFrequency.IMMEDIATE, 3, QuietHours.disabled()
        ));
    }

    @Test
    void shouldResolveScheduleConflictChannelAndBypassQuietHours() {
        NotificationPreference pref = NotificationPreference.createDefault(new UserId(100L));
        pref.update(
                true, true,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.EMAIL_ONLY,
                NotificationFrequency.IMMEDIATE, 3,
                QuietHours.of(true, LocalTime.of(22, 0), LocalTime.of(7, 0))
        );

        assertEquals(NotificationDeliveryChannel.EMAIL_ONLY,
                pref.getDeliveryChannelFor(NotificationType.SCHEDULE_CONFLICT));
        assertTrue(pref.isChannelActiveFor(NotificationType.SCHEDULE_CONFLICT, true, LocalTime.of(23, 0)));
    }

    @Test
    void shouldRejectEqualQuietHoursBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> QuietHours.of(true, LocalTime.of(22, 0), LocalTime.of(22, 0)));
    }

    @Test
    @DisplayName("Kiểm tra khung giờ yên tĩnh qua đêm (22:00 - 07:00)")
    void shouldHandleOvernightQuietHoursCorrectly() {
        QuietHours quietHours = QuietHours.of(true, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertTrue(quietHours.isInQuietHours(LocalTime.of(23, 30)));
        assertTrue(quietHours.isInQuietHours(LocalTime.of(5, 0)));
        assertTrue(quietHours.isInQuietHours(LocalTime.of(22, 0)));
        assertFalse(quietHours.isInQuietHours(LocalTime.of(7, 0)));
        assertFalse(quietHours.isInQuietHours(LocalTime.of(14, 0)));
    }
}
