package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyUpcomingDueTasksUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.task.CheckTaskDueReminderSentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDueReminderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.task.GetMyUpcomingDueTasksService;
import com.hrm.employeemanagement.application.service.task.TaskDueReminderApplicationService;

/**
 * Spring Configuration đăng ký các bean Use Case cho NCL-11-CN-004.
 * Giữ tầng Domain và Application hoàn toàn độc lập với Spring framework theo backend-guideline.
 */
@Configuration
public class TaskDueReminderUseCaseConfig {

    @Bean
    public ScanAndSendTaskDueRemindersUseCase scanAndSendTaskDueRemindersUseCase(
            LoadTaskDueReminderPort loadTaskDueReminderPort,
            CheckTaskDueReminderSentPort checkTaskDueReminderSentPort,
            LoadEmployeePort loadEmployeePort,
            @Autowired(required = false) SaveNotificationPort saveNotificationPort,
            @Autowired(required = false) CreateNotificationEventUseCase createNotificationEventUseCase,
            @Autowired(required = false) SaveAuditLogPort saveAuditLogPort
    ) {
        return new TaskDueReminderApplicationService(
                loadTaskDueReminderPort,
                checkTaskDueReminderSentPort,
                loadEmployeePort,
                saveNotificationPort,
                createNotificationEventUseCase,
                saveAuditLogPort
        );
    }

    @Bean
    public GetMyUpcomingDueTasksUseCase getMyUpcomingDueTasksUseCase(
            GetAuthenticatedUserPort getAuthenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadTaskDueReminderPort loadTaskDueReminderPort,
            @Autowired(required = false) SaveAuditLogInNewTransactionPort deniedAuditLogPort
    ) {
        return new GetMyUpcomingDueTasksService(
                getAuthenticatedUserPort,
                loadEmployeePort,
                loadTaskDueReminderPort,
                deniedAuditLogPort
        );
    }
}
