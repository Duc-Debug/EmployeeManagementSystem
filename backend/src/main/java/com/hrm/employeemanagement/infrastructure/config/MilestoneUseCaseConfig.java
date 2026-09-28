package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.milestone.CreateMilestoneUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.DeleteMilestoneUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.GetProjectMilestonesUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.UpdateMilestoneUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.milestone.DeleteMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.milestone.SaveMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.milestone.CreateMilestoneService;
import com.hrm.employeemanagement.application.service.milestone.DeleteMilestoneService;
import com.hrm.employeemanagement.application.service.milestone.GetProjectMilestonesService;
import com.hrm.employeemanagement.application.service.milestone.UpdateMilestoneService;
import com.hrm.employeemanagement.infrastructure.transaction.milestone.TransactionalCreateMilestoneUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.milestone.TransactionalDeleteMilestoneUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.milestone.TransactionalGetProjectMilestonesUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.milestone.TransactionalUpdateMilestoneUseCase;

@Configuration
public class MilestoneUseCaseConfig {

    @Bean
    public CreateMilestoneUseCase createMilestoneUseCase(
            LoadMilestonePort loadMilestonePort,
            SaveMilestonePort saveMilestonePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        CreateMilestoneService pureService = new CreateMilestoneService(
                loadMilestonePort,
                saveMilestonePort,
                loadProjectPort,
                loadTaskPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalCreateMilestoneUseCase(pureService);
    }

    @Bean
    public UpdateMilestoneUseCase updateMilestoneUseCase(
            LoadMilestonePort loadMilestonePort,
            SaveMilestonePort saveMilestonePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        UpdateMilestoneService pureService = new UpdateMilestoneService(
                loadMilestonePort,
                saveMilestonePort,
                loadProjectPort,
                loadTaskPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalUpdateMilestoneUseCase(pureService);
    }

    @Bean
    public GetProjectMilestonesUseCase getProjectMilestonesUseCase(
            LoadMilestonePort loadMilestonePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        GetProjectMilestonesService pureService = new GetProjectMilestonesService(
                loadMilestonePort,
                loadProjectPort,
                loadTaskPort,
                loadEmployeePort,
                loadUserPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalGetProjectMilestonesUseCase(pureService);
    }

    @Bean
    public DeleteMilestoneUseCase deleteMilestoneUseCase(
            LoadMilestonePort loadMilestonePort,
            DeleteMilestonePort deleteMilestonePort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        DeleteMilestoneService pureService = new DeleteMilestoneService(
                loadMilestonePort,
                deleteMilestonePort,
                loadProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalDeleteMilestoneUseCase(pureService);
    }
}
