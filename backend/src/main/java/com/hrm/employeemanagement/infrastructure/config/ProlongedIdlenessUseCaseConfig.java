package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.AcknowledgeProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.GetProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.allocation.idleness.ProlongedIdleStaffService;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.TransactionalProlongedIdleStaffService;

/**
 * Spring Configuration đăng ký các Use Case cho chức năng Cảnh báo nhân sự nhàn rỗi kéo dài (NCL-07-CN-006).
 */
@Configuration
public class ProlongedIdlenessUseCaseConfig {

    @Bean
    public ProlongedIdleStaffService pureProlongedIdleStaffService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadHolidaysPort loadHolidaysPort,
            SaveAuditLogInNewTransactionPort auditLogPort,
            SimulatedNotificationPort notificationPort
    ) {
        return new ProlongedIdleStaffService(
                authorizationService,
                loadEmployeePort,
                loadOrgUnitPort,
                loadCapacityThresholdPort,
                loadAllocationPort,
                loadApprovedLeavesPort,
                loadHolidaysPort,
                auditLogPort,
                notificationPort
        );
    }

    @Bean
    public TransactionalProlongedIdleStaffService transactionalProlongedIdleStaffService(
            ProlongedIdleStaffService pureProlongedIdleStaffService
    ) {
        return new TransactionalProlongedIdleStaffService(pureProlongedIdleStaffService);
    }

    @Bean
    public GetProlongedIdleStaffUseCase getProlongedIdleStaffUseCase(
            TransactionalProlongedIdleStaffService transactionalProlongedIdleStaffService
    ) {
        return transactionalProlongedIdleStaffService;
    }

    @Bean
    public AcknowledgeProlongedIdleStaffUseCase acknowledgeProlongedIdleStaffUseCase(
            TransactionalProlongedIdleStaffService transactionalProlongedIdleStaffService
    ) {
        return transactionalProlongedIdleStaffService;
    }
}
