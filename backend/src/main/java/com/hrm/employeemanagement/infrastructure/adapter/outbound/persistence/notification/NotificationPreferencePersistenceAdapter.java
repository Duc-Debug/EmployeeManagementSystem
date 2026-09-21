package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.GetOrCreateNotificationPreferencePort;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryChannel;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.NotificationPreferenceId;
import com.hrm.employeemanagement.domain.notification.QuietHours;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationPreferenceJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationPreferenceRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class NotificationPreferencePersistenceAdapter implements LoadNotificationPreferencePort,
        SaveNotificationPreferencePort, GetOrCreateNotificationPreferencePort {

    private final SpringDataNotificationPreferenceRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public NotificationPreferencePersistenceAdapter(SpringDataNotificationPreferenceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<NotificationPreference> findByUserId(UserId userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return repository.findByUserId(userId.value()).map(this::toDomain);
    }

    @Override
    public NotificationPreference getOrCreate(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        entityManager.createNativeQuery("SELECT id FROM users WHERE id = ? FOR UPDATE")
                .setParameter(1, userId.value())
                .getSingleResult();

        return repository.findByUserId(userId.value())
                .map(this::toDomain)
                .orElseGet(() -> save(NotificationPreference.createDefault(userId)));
    }

    @Override
    public NotificationPreference save(NotificationPreference preference) {
        if (preference == null) {
            return null;
        }
        NotificationPreferenceJpaEntity entity = toJpaEntity(preference);
        NotificationPreferenceJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private NotificationPreference toDomain(NotificationPreferenceJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        NotificationDeliveryChannel taskAssigned = parseChannel(entity.getTaskAssignedChannel(), NotificationDeliveryChannel.ALL);
        NotificationDeliveryChannel taskDueReminder = parseChannel(entity.getTaskDueReminderChannel(), NotificationDeliveryChannel.ALL);
        NotificationDeliveryChannel taskComment = parseChannel(entity.getTaskCommentChannel(), NotificationDeliveryChannel.IN_APP_ONLY);
        NotificationDeliveryChannel timesheetReminder = parseChannel(entity.getTimesheetReminderChannel(), NotificationDeliveryChannel.ALL);
        NotificationDeliveryChannel allocationChanged = parseChannel(entity.getAllocationChangedChannel(), NotificationDeliveryChannel.ALL);
        NotificationDeliveryChannel scheduleConflict = parseChannel(entity.getScheduleConflictChannel(), NotificationDeliveryChannel.ALL);

        NotificationFrequency frequency;
        try {
            frequency = entity.getFrequency() != null ? NotificationFrequency.valueOf(entity.getFrequency()) : NotificationFrequency.IMMEDIATE;
        } catch (IllegalArgumentException e) {
            frequency = NotificationFrequency.IMMEDIATE;
        }

        QuietHours quietHours = QuietHours.of(
                entity.isQuietHoursEnabled(),
                entity.getQuietHoursStart(),
                entity.getQuietHoursEnd()
        );

        return new NotificationPreference(
                entity.getId() != null ? new NotificationPreferenceId(entity.getId()) : null,
                new UserId(entity.getUserId()),
                entity.isInAppEnabled(),
                entity.isEmailEnabled(),
                taskAssigned,
                taskDueReminder,
                taskComment,
                timesheetReminder,
                allocationChanged,
                scheduleConflict,
                frequency,
                entity.getTaskDueReminderDays(),
                quietHours,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private NotificationPreferenceJpaEntity toJpaEntity(NotificationPreference domain) {
        NotificationPreferenceJpaEntity entity = new NotificationPreferenceJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId().value());
        }
        entity.setUserId(domain.getUserId().value());
        entity.setInAppEnabled(domain.isInAppEnabled());
        entity.setEmailEnabled(domain.isEmailEnabled());
        entity.setTaskAssignedChannel(domain.getTaskAssignedChannel().name());
        entity.setTaskDueReminderChannel(domain.getTaskDueReminderChannel().name());
        entity.setTaskCommentChannel(domain.getTaskCommentChannel().name());
        entity.setTimesheetReminderChannel(domain.getTimesheetReminderChannel().name());
        entity.setAllocationChangedChannel(domain.getAllocationChangedChannel().name());
        entity.setScheduleConflictChannel(domain.getScheduleConflictChannel().name());
        entity.setFrequency(domain.getFrequency().name());
        entity.setTaskDueReminderDays(domain.getTaskDueReminderDays());

        if (domain.getQuietHours() != null) {
            entity.setQuietHoursEnabled(domain.getQuietHours().enabled());
            entity.setQuietHoursStart(domain.getQuietHours().startTime());
            entity.setQuietHoursEnd(domain.getQuietHours().endTime());
        } else {
            entity.setQuietHoursEnabled(false);
            entity.setQuietHoursStart(null);
            entity.setQuietHoursEnd(null);
        }

        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    private NotificationDeliveryChannel parseChannel(String val, NotificationDeliveryChannel defaultChannel) {
        if (val == null) {
            return defaultChannel;
        }
        try {
            return NotificationDeliveryChannel.valueOf(val);
        } catch (IllegalArgumentException e) {
            return defaultChannel;
        }
    }
}
