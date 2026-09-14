package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.conflict.ScheduleConflictService;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;

@Configuration
public class ScheduleConflictUseCaseConfig {

    @Bean
    public ScheduleConflictService scheduleConflictService(
            LoadScheduleConflictPort loadConflictPort,
            SaveScheduleConflictPort saveConflictPort,
            SpringDataWeeklyProjectAllocationRepository allocationRepository,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort auditLogPort,
            SimulatedNotificationPort notificationPort
    ) {
        return new ScheduleConflictService(
                loadConflictPort,
                saveConflictPort,
                allocationRepository,
                loadApprovedLeavesPort,
                loadEmployeePort,
                loadProjectPort,
                loadOrgUnitPort,
                authorizationService,
                auditLogPort,
                notificationPort
        );
    }
}
