package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictReplacementPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.conflict.ScheduleConflictReplacementService;
import com.hrm.employeemanagement.application.service.conflict.ScheduleConflictService;
import com.hrm.employeemanagement.infrastructure.transaction.conflict.TransactionalScheduleConflictReplacementService;

@Configuration
public class ScheduleConflictUseCaseConfig {

    @Bean
    public ScheduleConflictService scheduleConflictService(
            LoadScheduleConflictPort loadConflictPort,
            SaveScheduleConflictPort saveConflictPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
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
                loadAllocationPort,
                loadApprovedLeavesPort,
                loadEmployeePort,
                loadProjectPort,
                loadOrgUnitPort,
                authorizationService,
                auditLogPort,
                notificationPort
        );
    }

    @Bean
    public TransactionalScheduleConflictReplacementService scheduleConflictReplacementService(
            AuthorizationService authorizationService,
            LoadScheduleConflictPort loadConflictPort,
            SaveScheduleConflictPort saveConflictPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadSkillPort loadSkillPort,
            EmployeeSkillRepository employeeSkillRepository,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            SaveScheduleConflictReplacementPort replacementPort,
            SaveAuditLogInNewTransactionPort auditLogPort,
            SimulatedNotificationPort notificationPort
    ) {
        ScheduleConflictReplacementService pureService = new ScheduleConflictReplacementService(
                authorizationService,
                loadConflictPort,
                saveConflictPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                loadSkillPort,
                employeeSkillRepository,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                loadApprovedLeavesPort,
                replacementPort,
                auditLogPort,
                notificationPort
        );

        return new TransactionalScheduleConflictReplacementService(pureService);
    }
}
