package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.notification.NotificationAuditLogRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.application.service.notification.CreateNotificationEventService;
import com.hrm.employeemanagement.application.service.notification.NotificationCenterApplicationService;
import com.hrm.employeemanagement.application.service.notification.PurgeExpiredNotificationsService;
import com.hrm.employeemanagement.infrastructure.transaction.notification.TransactionalCreateNotificationEventDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.notification.TransactionalNotificationCenterDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.notification.TransactionalPurgeExpiredNotificationsDecorator;

@Configuration
public class NotificationCenterUseCaseConfig {

    @Bean
    public TransactionalNotificationCenterDecorator transactionalNotificationCenterDecorator(
            NotificationRecipientRepositoryPort recipientRepositoryPort,
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationAuditLogRepositoryPort auditLogRepositoryPort,
            com.hrm.employeemanagement.application.port.outbound.notification.NotificationJsonSerializerPort jsonSerializerPort
    ) {
        NotificationCenterApplicationService pureService = new NotificationCenterApplicationService(
                recipientRepositoryPort,
                eventRepositoryPort,
                auditLogRepositoryPort,
                jsonSerializerPort
        );
        return new TransactionalNotificationCenterDecorator(pureService);
    }

    @Bean
    public TransactionalCreateNotificationEventDecorator transactionalCreateNotificationEventDecorator(
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationRecipientRepositoryPort recipientRepositoryPort
    ) {
        CreateNotificationEventService pureService = new CreateNotificationEventService(
                eventRepositoryPort,
                recipientRepositoryPort
        );
        return new TransactionalCreateNotificationEventDecorator(pureService);
    }

    @Bean
    public TransactionalPurgeExpiredNotificationsDecorator transactionalPurgeExpiredNotificationsDecorator(
            NotificationRecipientRepositoryPort recipientRepositoryPort,
            NotificationEventRepositoryPort eventRepositoryPort
    ) {
        PurgeExpiredNotificationsService pureService = new PurgeExpiredNotificationsService(
                recipientRepositoryPort,
                eventRepositoryPort
        );
        return new TransactionalPurgeExpiredNotificationsDecorator(pureService);
    }
}
