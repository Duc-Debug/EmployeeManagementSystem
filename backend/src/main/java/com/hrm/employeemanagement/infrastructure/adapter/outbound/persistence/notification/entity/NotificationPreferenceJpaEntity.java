package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "task_assigned_channel", nullable = false, length = 20)
    private String taskAssignedChannel = "ALL";

    @Column(name = "task_due_reminder_channel", nullable = false, length = 20)
    private String taskDueReminderChannel = "ALL";

    @Column(name = "task_comment_channel", nullable = false, length = 20)
    private String taskCommentChannel = "IN_APP_ONLY";

    @Column(name = "timesheet_reminder_channel", nullable = false, length = 20)
    private String timesheetReminderChannel = "ALL";

    @Column(name = "allocation_changed_channel", nullable = false, length = 20)
    private String allocationChangedChannel = "ALL";

    @Column(name = "schedule_conflict_channel", nullable = false, length = 20)
    private String scheduleConflictChannel = "ALL";

    @Column(name = "frequency", nullable = false, length = 30)
    private String frequency = "IMMEDIATE";

    @Column(name = "task_due_reminder_days", nullable = false)
    private int taskDueReminderDays = 3;

    @Column(name = "quiet_hours_enabled", nullable = false)
    private boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public NotificationPreferenceJpaEntity() {
    }

    public NotificationPreferenceJpaEntity(
            Long id,
            Long userId,
            boolean inAppEnabled,
            boolean emailEnabled,
            String taskAssignedChannel,
            String taskDueReminderChannel,
            String taskCommentChannel,
            String timesheetReminderChannel,
            String allocationChangedChannel,
            String scheduleConflictChannel,
            String frequency,
            int taskDueReminderDays,
            boolean quietHoursEnabled,
            LocalTime quietHoursStart,
            LocalTime quietHoursEnd,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
        this.taskAssignedChannel = taskAssignedChannel;
        this.taskDueReminderChannel = taskDueReminderChannel;
        this.taskCommentChannel = taskCommentChannel;
        this.timesheetReminderChannel = timesheetReminderChannel;
        this.allocationChangedChannel = allocationChangedChannel;
        this.scheduleConflictChannel = scheduleConflictChannel;
        this.frequency = frequency;
        this.taskDueReminderDays = taskDueReminderDays;
        this.quietHoursEnabled = quietHoursEnabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public String getTaskAssignedChannel() {
        return taskAssignedChannel;
    }

    public void setTaskAssignedChannel(String taskAssignedChannel) {
        this.taskAssignedChannel = taskAssignedChannel;
    }

    public String getTaskDueReminderChannel() {
        return taskDueReminderChannel;
    }

    public void setTaskDueReminderChannel(String taskDueReminderChannel) {
        this.taskDueReminderChannel = taskDueReminderChannel;
    }

    public String getTaskCommentChannel() {
        return taskCommentChannel;
    }

    public void setTaskCommentChannel(String taskCommentChannel) {
        this.taskCommentChannel = taskCommentChannel;
    }

    public String getTimesheetReminderChannel() {
        return timesheetReminderChannel;
    }

    public void setTimesheetReminderChannel(String timesheetReminderChannel) {
        this.timesheetReminderChannel = timesheetReminderChannel;
    }

    public String getAllocationChangedChannel() {
        return allocationChangedChannel;
    }

    public void setAllocationChangedChannel(String allocationChangedChannel) {
        this.allocationChangedChannel = allocationChangedChannel;
    }

    public String getScheduleConflictChannel() {
        return scheduleConflictChannel;
    }

    public void setScheduleConflictChannel(String scheduleConflictChannel) {
        this.scheduleConflictChannel = scheduleConflictChannel;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public int getTaskDueReminderDays() {
        return taskDueReminderDays;
    }

    public void setTaskDueReminderDays(int taskDueReminderDays) {
        this.taskDueReminderDays = taskDueReminderDays;
    }

    public boolean isQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
