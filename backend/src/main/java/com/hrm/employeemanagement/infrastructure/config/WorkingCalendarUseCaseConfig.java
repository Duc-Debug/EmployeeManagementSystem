package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayCommandPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayQueryPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.SaveWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.calendar.WorkingCalendarService;
import com.hrm.employeemanagement.infrastructure.transaction.calendar.TransactionalWorkingCalendarServiceDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkingCalendarUseCaseConfig {

    @Bean
    public TransactionalWorkingCalendarServiceDecorator workingCalendarServiceDecorator(
            AuthorizationService authorizationService,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveWorkingCalendarPort saveWorkingCalendarPort,
            HolidayQueryPort holidayQueryPort,
            HolidayCommandPort holidayCommandPort,
            SaveAuditLogPort saveAuditLogPort) {

        WorkingCalendarService pureService = new WorkingCalendarService(
                authorizationService,
                loadWorkingCalendarPort,
                saveWorkingCalendarPort,
                holidayQueryPort,
                holidayCommandPort,
                saveAuditLogPort
        );

        return new TransactionalWorkingCalendarServiceDecorator(pureService);
    }
}
