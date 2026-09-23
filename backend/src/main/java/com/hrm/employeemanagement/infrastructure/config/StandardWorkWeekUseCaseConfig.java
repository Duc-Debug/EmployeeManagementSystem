package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.workweek.LoadStandardWorkWeekPort;
import com.hrm.employeemanagement.application.port.outbound.workweek.SaveStandardWorkWeekPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.workweek.StandardWorkWeekService;
import com.hrm.employeemanagement.infrastructure.transaction.workweek.TransactionalStandardWorkWeekServiceDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StandardWorkWeekUseCaseConfig {

    @Bean
    public TransactionalStandardWorkWeekServiceDecorator standardWorkWeekServiceDecorator(
            AuthorizationService authorizationService,
            LoadStandardWorkWeekPort loadStandardWorkWeekPort,
            SaveStandardWorkWeekPort saveStandardWorkWeekPort,
            LoadOrgUnitPort loadOrgUnitPort,
            SaveAuditLogPort saveAuditLogPort) {

        StandardWorkWeekService pureService = new StandardWorkWeekService(
                authorizationService,
                loadStandardWorkWeekPort,
                saveStandardWorkWeekPort,
                loadOrgUnitPort,
                saveAuditLogPort
        );

        return new TransactionalStandardWorkWeekServiceDecorator(pureService);
    }
}

