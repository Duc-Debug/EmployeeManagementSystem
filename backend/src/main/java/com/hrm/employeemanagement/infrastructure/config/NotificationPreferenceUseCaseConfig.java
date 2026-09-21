package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPreferencePort;
import com.hrm.employeemanagement.application.service.notification.NotificationPreferenceApplicationService;
import com.hrm.employeemanagement.infrastructure.transaction.notification.TransactionalNotificationPreferenceDecorator;

@Configuration
public class NotificationPreferenceUseCaseConfig {

    @Bean
    public TransactionalNotificationPreferenceDecorator transactionalNotificationPreferenceDecorator(
            LoadNotificationPreferencePort loadNotificationPreferencePort,
            SaveNotificationPreferencePort saveNotificationPreferencePort
    ) {
        NotificationPreferenceApplicationService pureService = new NotificationPreferenceApplicationService(
                loadNotificationPreferencePort,
                saveNotificationPreferencePort
        );
        return new TransactionalNotificationPreferenceDecorator(pureService);
    }
}
