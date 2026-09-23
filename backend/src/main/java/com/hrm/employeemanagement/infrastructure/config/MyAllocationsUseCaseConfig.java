package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.allocation.ConfirmScheduleViewedUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetMyAllocationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.ProvideScheduleFeedbackUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadMyAllocationsPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.allocation.ConfirmScheduleViewedService;
import com.hrm.employeemanagement.application.service.allocation.GetMyAllocationsService;
import com.hrm.employeemanagement.application.service.allocation.ProvideScheduleFeedbackService;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.TransactionalConfirmScheduleViewedUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.TransactionalGetMyAllocationsUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.TransactionalProvideScheduleFeedbackUseCase;

@Configuration
public class MyAllocationsUseCaseConfig {

    @Bean
    public GetMyAllocationsUseCase getMyAllocationsUseCase(
            GetAuthenticatedUserPort authenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadMyAllocationsPort loadMyAllocationsPort,
            ScheduleConfirmationPort scheduleConfirmationPort) {
        GetMyAllocationsService pureService = new GetMyAllocationsService(
                authenticatedUserPort,
                loadEmployeePort,
                loadMyAllocationsPort,
                scheduleConfirmationPort
        );
        return new TransactionalGetMyAllocationsUseCase(pureService);
    }

    @Bean
    public ConfirmScheduleViewedUseCase confirmScheduleViewedUseCase(
            GetAuthenticatedUserPort authenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadMyAllocationsPort loadMyAllocationsPort,
            ScheduleConfirmationPort scheduleConfirmationPort) {
        ConfirmScheduleViewedService pureService = new ConfirmScheduleViewedService(
                authenticatedUserPort,
                loadEmployeePort,
                loadMyAllocationsPort,
                scheduleConfirmationPort
        );
        return new TransactionalConfirmScheduleViewedUseCase(pureService);
    }

    @Bean
    public ProvideScheduleFeedbackUseCase provideScheduleFeedbackUseCase(
            GetAuthenticatedUserPort authenticatedUserPort,
            ScheduleConfirmationPort scheduleConfirmationPort) {
        ProvideScheduleFeedbackService pureService = new ProvideScheduleFeedbackService(
                authenticatedUserPort,
                scheduleConfirmationPort
        );
        return new TransactionalProvideScheduleFeedbackUseCase(pureService);
    }
}
