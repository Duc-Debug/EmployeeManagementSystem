package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.timesheet.CreateWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.DeleteWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetMyAssignedTasksForWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.SubmitWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.UpdateWorkLogUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.DeleteTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.timesheet.CreateWorkLogService;
import com.hrm.employeemanagement.application.service.timesheet.DeleteWorkLogService;
import com.hrm.employeemanagement.application.service.timesheet.GetMyAssignedTasksForWorkLogService;
import com.hrm.employeemanagement.application.service.timesheet.GetWeeklyTimesheetService;
import com.hrm.employeemanagement.application.service.timesheet.SubmitWeeklyTimesheetService;
import com.hrm.employeemanagement.application.service.timesheet.UpdateWorkLogService;
import com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalCreateWorkLogUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalDeleteWorkLogUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalGetMyAssignedTasksForWorkLogUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalGetWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalSubmitWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalUpdateWorkLogUseCase;

@Configuration
public class TimesheetUseCaseConfig {

    @Bean
    public CreateWorkLogUseCase createWorkLogUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        CreateWorkLogService pureService = new CreateWorkLogService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                saveAuditLogPort,
                authorizationService
        );
        return new TransactionalCreateWorkLogUseCase(pureService);
    }

    @Bean
    public UpdateWorkLogUseCase updateWorkLogUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        UpdateWorkLogService pureService = new UpdateWorkLogService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                saveAuditLogPort,
                authorizationService
        );
        return new TransactionalUpdateWorkLogUseCase(pureService);
    }

    @Bean
    public DeleteWorkLogUseCase deleteWorkLogUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            DeleteTimesheetEntryPort deleteTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        DeleteWorkLogService pureService = new DeleteWorkLogService(
                loadEmployeePort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadTimesheetEntryPort,
                deleteTimesheetEntryPort,
                saveAuditLogPort,
                authorizationService
        );
        return new TransactionalDeleteWorkLogUseCase(pureService);
    }

    @Bean
    public GetWeeklyTimesheetUseCase getWeeklyTimesheetUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTimesheetPort loadTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            AuthorizationService authorizationService) {
        GetWeeklyTimesheetService pureService = new GetWeeklyTimesheetService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTimesheetPort,
                loadTimesheetEntryPort,
                authorizationService
        );
        return new TransactionalGetWeeklyTimesheetUseCase(pureService);
    }

    @Bean
    public GetMyAssignedTasksForWorkLogUseCase getMyAssignedTasksForWorkLogUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            AuthorizationService authorizationService) {
        GetMyAssignedTasksForWorkLogService pureService = new GetMyAssignedTasksForWorkLogService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                authorizationService
        );
        return new TransactionalGetMyAssignedTasksForWorkLogUseCase(pureService);
    }

    @Bean
    public SubmitWeeklyTimesheetUseCase submitWeeklyTimesheetUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        SubmitWeeklyTimesheetService pureService = new SubmitWeeklyTimesheetService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                saveAuditLogPort,
                authorizationService
        );
        return new TransactionalSubmitWeeklyTimesheetUseCase(pureService);
    }

    @Bean
    public com.hrm.employeemanagement.application.port.inbound.timesheet.SendTimesheetRemindersUseCase sendTimesheetRemindersUseCase(
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadEmployeePort loadEmployeePort,
            com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort saveNotificationPort,
            com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetHistoryPort saveTimesheetHistoryPort,
            com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort loadWorkingCalendarPort,
            com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort loadHolidaysPort) {
        com.hrm.employeemanagement.application.service.timesheet.SendTimesheetRemindersService pureService = new com.hrm.employeemanagement.application.service.timesheet.SendTimesheetRemindersService(
                loadTimesheetPort,
                saveTimesheetPort,
                loadEmployeePort,
                saveNotificationPort,
                saveTimesheetHistoryPort,
                loadWorkingCalendarPort,
                loadHolidaysPort,
                java.time.Clock.systemDefaultZone()
        );
        return new com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalSendTimesheetRemindersUseCase(pureService);
    }

    @Bean
    public com.hrm.employeemanagement.application.port.inbound.timesheet.SaveWeeklyTimesheetGridUseCase saveWeeklyTimesheetGridUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            DeleteTimesheetEntryPort deleteTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            GetWeeklyTimesheetUseCase getWeeklyTimesheetUseCase,
            AuthorizationService authorizationService) {
        com.hrm.employeemanagement.application.service.timesheet.SaveWeeklyTimesheetGridService pureService =
                new com.hrm.employeemanagement.application.service.timesheet.SaveWeeklyTimesheetGridService(
                        loadEmployeePort,
                        loadProjectPort,
                        loadTaskPort,
                        loadTaskAssignmentPort,
                        loadTimesheetPort,
                        saveTimesheetPort,
                        loadTimesheetEntryPort,
                        saveTimesheetEntryPort,
                        deleteTimesheetEntryPort,
                        saveAuditLogPort,
                        getWeeklyTimesheetUseCase,
                        authorizationService
                );
        return new com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalSaveWeeklyTimesheetGridUseCase(pureService);
    }

    @Bean
    public com.hrm.employeemanagement.application.port.inbound.timesheet.ApproveTimesheetUseCase approveTimesheetUseCase(
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetAuditLogPort saveTimesheetAuditLogPort,
            AuthorizationService authorizationService) {
        com.hrm.employeemanagement.application.service.timesheet.ApproveTimesheetService pureService = new com.hrm.employeemanagement.application.service.timesheet.ApproveTimesheetService(
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadProjectPort,
                loadTaskPort,
                loadEmployeePort,
                saveTimesheetAuditLogPort,
                authorizationService
        );
        return new com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalApproveTimesheetUseCase(pureService);
    }

    @Bean
    public com.hrm.employeemanagement.application.port.inbound.timesheet.GetPendingApprovalsUseCase getPendingApprovalsUseCase(
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService) {
        com.hrm.employeemanagement.application.service.timesheet.GetPendingApprovalsService pureService = new com.hrm.employeemanagement.application.service.timesheet.GetPendingApprovalsService(
                loadTimesheetEntryPort,
                loadProjectPort,
                loadTaskPort,
                loadEmployeePort,
                authorizationService
        );
        return new com.hrm.employeemanagement.infrastructure.transaction.timesheet.TransactionalGetPendingApprovalsUseCase(pureService);
    }
}