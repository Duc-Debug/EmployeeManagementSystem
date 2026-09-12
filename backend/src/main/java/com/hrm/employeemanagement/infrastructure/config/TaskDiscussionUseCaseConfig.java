package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.DeleteTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.SaveTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.notification.NotificationApplicationService;
import com.hrm.employeemanagement.application.service.task.comment.TaskCommentApplicationService;

@Configuration
public class TaskDiscussionUseCaseConfig {

    @Bean
    public TaskCommentApplicationService taskCommentApplicationService(
            LoadTaskPort loadTaskPort,
            LoadTaskCommentPort loadTaskCommentPort,
            SaveTaskCommentPort saveTaskCommentPort,
            DeleteTaskCommentPort deleteTaskCommentPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            TaskAttachmentStoragePort taskAttachmentStoragePort) {
        return new TaskCommentApplicationService(
                loadTaskPort,
                loadTaskCommentPort,
                saveTaskCommentPort,
                deleteTaskCommentPort,
                loadUserPort,
                loadEmployeePort,
                saveNotificationPort,
                taskAttachmentStoragePort);
    }

    @Bean
    public NotificationApplicationService notificationApplicationService(
            LoadNotificationPort loadNotificationPort,
            SaveNotificationPort saveNotificationPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort) {
        return new NotificationApplicationService(
                loadNotificationPort,
                saveNotificationPort,
                loadUserPort,
                loadEmployeePort);
    }
}
