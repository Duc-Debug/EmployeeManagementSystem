package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.notification.NotificationPreferenceValidationException;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Aggregate Root quản lý cấu hình kênh và tần suất nhận thông báo (NCL-11-CN-002).
 * Pure Java, tuân thủ nghiêm ngặt nguyên tắc DDD & Clean Architecture.
 */
public class NotificationPreference {

    public static final int DEFAULT_TASK_DUE_REMINDER_DAYS = 3;
    public static final int MIN_TASK_DUE_REMINDER_DAYS = 1;
    public static final int MAX_TASK_DUE_REMINDER_DAYS = 14;

    private NotificationPreferenceId id;
    private final UserId userId;
    private boolean inAppEnabled;
    private boolean emailEnabled;
    private NotificationDeliveryChannel taskAssignedChannel;
    private NotificationDeliveryChannel taskDueReminderChannel;
    private NotificationDeliveryChannel taskCommentChannel;
    private NotificationDeliveryChannel timesheetReminderChannel;
    private NotificationDeliveryChannel allocationChangedChannel;
    private NotificationDeliveryChannel scheduleConflictChannel;
    private NotificationFrequency frequency;
    private int taskDueReminderDays;
    private QuietHours quietHours;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NotificationPreference(
            NotificationPreferenceId id,
            UserId userId,
            boolean inAppEnabled,
            boolean emailEnabled,
            NotificationDeliveryChannel taskAssignedChannel,
            NotificationDeliveryChannel taskDueReminderChannel,
            NotificationDeliveryChannel taskCommentChannel,
            NotificationDeliveryChannel timesheetReminderChannel,
            NotificationDeliveryChannel allocationChangedChannel,
            NotificationDeliveryChannel scheduleConflictChannel,
            NotificationFrequency frequency,
            int taskDueReminderDays,
            QuietHours quietHours,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "UserId không được để trống");
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
        this.taskAssignedChannel = taskAssignedChannel != null ? taskAssignedChannel : NotificationDeliveryChannel.ALL;
        this.taskDueReminderChannel = taskDueReminderChannel != null ? taskDueReminderChannel : NotificationDeliveryChannel.ALL;
        this.taskCommentChannel = taskCommentChannel != null ? taskCommentChannel : NotificationDeliveryChannel.IN_APP_ONLY;
        this.timesheetReminderChannel = timesheetReminderChannel != null ? timesheetReminderChannel : NotificationDeliveryChannel.ALL;
        this.allocationChangedChannel = allocationChangedChannel != null ? allocationChangedChannel : NotificationDeliveryChannel.ALL;
        this.scheduleConflictChannel = scheduleConflictChannel != null ? scheduleConflictChannel : NotificationDeliveryChannel.ALL;
        this.frequency = frequency != null ? frequency : NotificationFrequency.IMMEDIATE;
        this.taskDueReminderDays = validateReminderDays(taskDueReminderDays);
        this.quietHours = quietHours != null ? quietHours : QuietHours.disabled();
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();

        validateCriticalChannelConstraints();
    }

    public static NotificationPreference createDefault(UserId userId) {
        return new NotificationPreference(
                null,
                userId,
                true,
                true,
                NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL,
                NotificationFrequency.IMMEDIATE,
                DEFAULT_TASK_DUE_REMINDER_DAYS,
                QuietHours.disabled(),
                0L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void update(
            boolean inAppEnabled,
            boolean emailEnabled,
            NotificationDeliveryChannel taskAssignedChannel,
            NotificationDeliveryChannel taskDueReminderChannel,
            NotificationDeliveryChannel taskCommentChannel,
            NotificationDeliveryChannel timesheetReminderChannel,
            NotificationDeliveryChannel allocationChangedChannel,
            NotificationDeliveryChannel scheduleConflictChannel,
            NotificationFrequency frequency,
            int taskDueReminderDays,
            QuietHours quietHours
    ) {
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
        this.taskAssignedChannel = taskAssignedChannel != null ? taskAssignedChannel : NotificationDeliveryChannel.ALL;
        this.taskDueReminderChannel = taskDueReminderChannel != null ? taskDueReminderChannel : NotificationDeliveryChannel.ALL;
        this.taskCommentChannel = taskCommentChannel != null ? taskCommentChannel : NotificationDeliveryChannel.IN_APP_ONLY;
        this.timesheetReminderChannel = timesheetReminderChannel != null ? timesheetReminderChannel : NotificationDeliveryChannel.ALL;
        this.allocationChangedChannel = allocationChangedChannel != null ? allocationChangedChannel : NotificationDeliveryChannel.ALL;
        this.scheduleConflictChannel = scheduleConflictChannel != null ? scheduleConflictChannel : NotificationDeliveryChannel.ALL;
        this.frequency = frequency != null ? frequency : NotificationFrequency.IMMEDIATE;
        this.taskDueReminderDays = validateReminderDays(taskDueReminderDays);
        this.quietHours = quietHours != null ? quietHours : QuietHours.disabled();
        this.updatedAt = LocalDateTime.now();

        validateCriticalChannelConstraints();
    }

    public void resetToDefault() {
        this.inAppEnabled = true;
        this.emailEnabled = true;
        this.taskAssignedChannel = NotificationDeliveryChannel.ALL;
        this.taskDueReminderChannel = NotificationDeliveryChannel.ALL;
        this.taskCommentChannel = NotificationDeliveryChannel.IN_APP_ONLY;
        this.timesheetReminderChannel = NotificationDeliveryChannel.ALL;
        this.allocationChangedChannel = NotificationDeliveryChannel.ALL;
        this.scheduleConflictChannel = NotificationDeliveryChannel.ALL;
        this.frequency = NotificationFrequency.IMMEDIATE;
        this.taskDueReminderDays = DEFAULT_TASK_DUE_REMINDER_DAYS;
        this.quietHours = QuietHours.disabled();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * BR-03: Cảnh báo trọng yếu (Xung đột lịch & Phân bổ) bắt buộc phải bật ít nhất 1 kênh.
     */
    private void validateCriticalChannelConstraints() {
        if (!hasActiveChannel(this.scheduleConflictChannel)) {
            throw new NotificationPreferenceValidationException(
                    "Cảnh báo xung đột lịch là thông tin trọng yếu, bắt buộc phải bật ít nhất 1 kênh nhận thông báo."
            );
        }
        if (!hasActiveChannel(this.allocationChangedChannel)) {
            throw new NotificationPreferenceValidationException(
                    "Cảnh báo thay đổi phân bổ nhân sự là thông tin trọng yếu, bắt buộc phải bật ít nhất 1 kênh nhận thông báo."
            );
        }
    }

    private boolean hasActiveChannel(NotificationDeliveryChannel channel) {
        return channel != null
                && channel != NotificationDeliveryChannel.NONE
                && ((inAppEnabled && channel.isInAppEnabled())
                || (emailEnabled && channel.isEmailEnabled()));
    }

    private static int validateReminderDays(int days) {
        if (days < MIN_TASK_DUE_REMINDER_DAYS || days > MAX_TASK_DUE_REMINDER_DAYS) {
            throw new NotificationPreferenceValidationException(
                    String.format("Số ngày nhắc việc sắp đến hạn phải nằm trong khoảng từ %d đến %d ngày.",
                            MIN_TASK_DUE_REMINDER_DAYS, MAX_TASK_DUE_REMINDER_DAYS)
            );
        }
        return days;
    }

    /**
     * Kiểm tra xem kênh cụ thể (In-App hay Email) có được kích hoạt cho loại thông báo này hay không.
     */
    public boolean isChannelActiveFor(NotificationType type, boolean isEmailRequest, LocalTime currentTime) {
        if (isEmailRequest) {
            if (!this.emailEnabled) {
                return false;
            }
            if (this.quietHours.isInQuietHours(currentTime)) {
                // Trong giờ yên tĩnh: hoãn gửi email trừ khi là cảnh báo xung đột khẩn cấp
                if (type != NotificationType.ALLOCATION_CHANGED
                        && type != NotificationType.SCHEDULE_CONFLICT) {
                    return false;
                }
            }
            return getDeliveryChannelFor(type).isEmailEnabled();
        } else {
            if (!this.inAppEnabled) {
                return false;
            }
            return getDeliveryChannelFor(type).isInAppEnabled();
        }
    }

    public NotificationDeliveryChannel getDeliveryChannelFor(NotificationType type) {
        if (type == null) {
            return NotificationDeliveryChannel.ALL;
        }
        return switch (type) {
            case TASK_ASSIGNED -> taskAssignedChannel;
            case TASK_DUE_REMINDER -> taskDueReminderChannel;
            case TASK_COMMENT, TASK_MENTION -> taskCommentChannel;
            case TIMESHEET_REMINDER -> timesheetReminderChannel;
            case SCHEDULE_CONFLICT -> scheduleConflictChannel;
            case ALLOCATION_CHANGED -> allocationChangedChannel;
            case NOTIFICATION_DIGEST -> NotificationDeliveryChannel.ALL;
        };
    }

    // Getters and Setters
    public NotificationPreferenceId getId() {
        return id;
    }

    public void setId(NotificationPreferenceId id) {
        this.id = id;
    }

    public UserId getUserId() {
        return userId;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public NotificationDeliveryChannel getTaskAssignedChannel() {
        return taskAssignedChannel;
    }

    public NotificationDeliveryChannel getTaskDueReminderChannel() {
        return taskDueReminderChannel;
    }

    public NotificationDeliveryChannel getTaskCommentChannel() {
        return taskCommentChannel;
    }

    public NotificationDeliveryChannel getTimesheetReminderChannel() {
        return timesheetReminderChannel;
    }

    public NotificationDeliveryChannel getAllocationChangedChannel() {
        return allocationChangedChannel;
    }

    public NotificationDeliveryChannel getScheduleConflictChannel() {
        return scheduleConflictChannel;
    }

    public NotificationFrequency getFrequency() {
        return frequency;
    }

    public int getTaskDueReminderDays() {
        return taskDueReminderDays;
    }

    public QuietHours getQuietHours() {
        return quietHours;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
