package com.hrm.employeemanagement.application.service.notification;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.notification.NotificationPreferenceResult;
import com.hrm.employeemanagement.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.ResetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.UpdateNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPreferencePort;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.QuietHours;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application Service điều phối cấu hình kênh và tần suất nhận thông báo (NCL-11-CN-002).
 * Pure Java, không sử dụng Spring annotations trực tiếp theo backend-guideline.
 */
public class NotificationPreferenceApplicationService implements
        GetNotificationPreferenceUseCase,
        UpdateNotificationPreferenceUseCase,
        ResetNotificationPreferenceUseCase {

    private final LoadNotificationPreferencePort loadNotificationPreferencePort;
    private final SaveNotificationPreferencePort saveNotificationPreferencePort;

    public NotificationPreferenceApplicationService(
            LoadNotificationPreferencePort loadNotificationPreferencePort,
            SaveNotificationPreferencePort saveNotificationPreferencePort
    ) {
        this.loadNotificationPreferencePort = Objects.requireNonNull(loadNotificationPreferencePort, "loadNotificationPreferencePort must not be null");
        this.saveNotificationPreferencePort = Objects.requireNonNull(saveNotificationPreferencePort, "saveNotificationPreferencePort must not be null");
    }

    @Override
    public NotificationPreferenceResult getMyPreference(Long currentUserId) {
        UserId userId = requireUserId(currentUserId);
        NotificationPreference preference = loadNotificationPreferencePort.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference defaultPref = NotificationPreference.createDefault(userId);
                    return saveNotificationPreferencePort.save(defaultPref);
                });
        return NotificationPreferenceResult.fromDomain(preference);
    }

    @Override
    public NotificationPreferenceResult updateMyPreference(Long currentUserId, UpdateNotificationPreferenceCommand command) {
        UserId userId = requireUserId(currentUserId);
        Objects.requireNonNull(command, "UpdateNotificationPreferenceCommand không được null");

        NotificationPreference preference = loadNotificationPreferencePort.findByUserId(userId)
                .orElseGet(() -> NotificationPreference.createDefault(userId));

        QuietHours quietHours = QuietHours.of(
                command.quietHoursEnabled(),
                command.quietHoursStart(),
                command.quietHoursEnd()
        );

        preference.update(
                command.inAppEnabled(),
                command.emailEnabled(),
                command.taskAssignedChannel(),
                command.taskDueReminderChannel(),
                command.taskCommentChannel(),
                command.timesheetReminderChannel(),
                command.allocationChangedChannel(),
                command.scheduleConflictChannel(),
                command.frequency(),
                command.taskDueReminderDays(),
                quietHours
        );

        NotificationPreference saved = saveNotificationPreferencePort.save(preference);
        return NotificationPreferenceResult.fromDomain(saved);
    }

    @Override
    public NotificationPreferenceResult resetMyPreference(Long currentUserId) {
        UserId userId = requireUserId(currentUserId);
        NotificationPreference preference = loadNotificationPreferencePort.findByUserId(userId)
                .orElseGet(() -> NotificationPreference.createDefault(userId));

        preference.resetToDefault();
        NotificationPreference saved = saveNotificationPreferencePort.save(preference);
        return NotificationPreferenceResult.fromDomain(saved);
    }

    private UserId requireUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("currentUserId không được null");
        }
        return new UserId(currentUserId);
    }
}
