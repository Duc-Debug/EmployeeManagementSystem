package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.LoadResourceReservationPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.SaveResourceReservationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.reservation.ResourceReservationService;
import com.hrm.employeemanagement.infrastructure.transaction.reservation.TransactionalResourceReservationServiceDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceReservationUseCaseConfig {

    @Bean
    public TransactionalResourceReservationServiceDecorator resourceReservationService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadResourceReservationPort loadReservationPort,
            SaveResourceReservationPort saveReservationPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        ResourceReservationService service = new ResourceReservationService(
                authorizationService,
                loadEmployeePort,
                loadProjectPort,
                loadAllocationPort,
                saveAllocationPort,
                loadWeeklyAvailabilityPort,
                loadReservationPort,
                saveReservationPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                loadUserPort,
                loadOrgUnitPort
        );
        return new TransactionalResourceReservationServiceDecorator(service);
    }
}
