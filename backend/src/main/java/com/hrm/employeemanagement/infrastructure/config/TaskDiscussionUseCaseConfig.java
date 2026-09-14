package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.DeleteTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.SaveTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.notification.NotificationApplicationService;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.task.comment.TaskCommentApplicationService;
import com.hrm.employeemanagement.application.service.task.comment.TaskDiscussionAccessService;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalTaskCommentServiceDecorator;

@Configuration
public class TaskDiscussionUseCaseConfig {

    @Bean
    public TaskDiscussionAccessService taskDiscussionAccessService(
            AuthorizationService authorizationService, LoadTaskPort loadTaskPort,
            LoadProjectPort loadProjectPort, LoadUserPort loadUserPort, LoadEmployeePort loadEmployeePort) {
        return new TaskDiscussionAccessService(
                authorizationService, loadTaskPort, loadProjectPort, loadUserPort, loadEmployeePort);
    }

    @Bean
    public TransactionalTaskCommentServiceDecorator taskCommentApplicationService(
            LoadTaskCommentPort loadTaskCommentPort,
            SaveTaskCommentPort saveTaskCommentPort,
            DeleteTaskCommentPort deleteTaskCommentPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            TaskAttachmentStoragePort taskAttachmentStoragePort,
            TaskDiscussionAccessService accessService) {
        TaskCommentApplicationService service = new TaskCommentApplicationService(
                loadTaskCommentPort,
                saveTaskCommentPort,
                deleteTaskCommentPort,
                loadUserPort,
                loadEmployeePort,
                saveNotificationPort,
                accessService);
        return new TransactionalTaskCommentServiceDecorator(service, loadTaskCommentPort, taskAttachmentStoragePort);
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
