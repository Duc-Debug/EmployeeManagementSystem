package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.task.AssignTaskUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.CloneProjectWbsUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyTasksUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectWbsUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.SetTaskBudgetUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.task.AssignTaskService;
import com.hrm.employeemanagement.application.service.task.CloneProjectWbsService;
import com.hrm.employeemanagement.application.service.task.CreateTaskService;
import com.hrm.employeemanagement.application.service.task.GetMyTasksService;
import com.hrm.employeemanagement.application.service.task.GetProjectWbsService;
import com.hrm.employeemanagement.application.service.task.SetTaskBudgetService;
import com.hrm.employeemanagement.application.service.task.UpdateTaskService;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalAssignTaskUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalCloneProjectWbsUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalCreateTaskUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalGetMyTasksUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalGetProjectWbsUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalSetTaskBudgetUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalUpdateTaskUseCase;

@Configuration
public class TaskUseCaseConfig {

    @Bean
    public CreateTaskUseCase createTaskUseCase(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            SaveTaskAssignmentPort saveTaskAssignmentPort,
            SaveProjectMemberPort saveProjectMemberPort,
            LoadProjectPort loadProjectPort,
            SaveProjectPort saveProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        CreateTaskService pureService = new CreateTaskService(
                loadTaskPort,
                saveTaskPort,
                saveTaskAssignmentPort,
                saveProjectMemberPort,
                loadProjectPort,
                saveProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalCreateTaskUseCase(pureService);
    }

    @Bean
    public AssignTaskUseCase assignTaskUseCase(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            SaveTaskAssignmentPort saveTaskAssignmentPort,
            LoadProjectPort loadProjectPort,
            SaveProjectMemberPort saveProjectMemberPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        AssignTaskService pureService = new AssignTaskService(
                loadTaskPort,
                saveTaskPort,
                loadTaskAssignmentPort,
                saveTaskAssignmentPort,
                loadProjectPort,
                saveProjectMemberPort,
                loadEmployeePort,
                saveAuditLogPort,
                authorizationService);
        return new TransactionalAssignTaskUseCase(pureService);
    }

    @Bean
    public GetMyTasksUseCase getMyTasksUseCase(
            GetAuthenticatedUserPort authenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTaskPort loadTaskPort,
            LoadProjectPort loadProjectPort) {
        GetMyTasksService pureService = new GetMyTasksService(
                authenticatedUserPort,
                loadEmployeePort,
                loadTaskAssignmentPort,
                loadTaskPort,
                loadProjectPort);
        return new TransactionalGetMyTasksUseCase(pureService);
    }

    @Bean
    public UpdateTaskUseCase updateTaskUseCase(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        UpdateTaskService pureService = new UpdateTaskService(
                loadTaskPort,
                saveTaskPort,
                loadProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalUpdateTaskUseCase(pureService);
    }

    @Bean
    public GetProjectWbsUseCase getProjectWbsUseCase(
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        GetProjectWbsService pureService = new GetProjectWbsService(
                loadTaskPort,
                loadTaskAssignmentPort,
                loadProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalGetProjectWbsUseCase(pureService);
    }

    @Bean
    public SetTaskBudgetUseCase setTaskBudgetUseCase(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        SetTaskBudgetService pureService = new SetTaskBudgetService(
                loadTaskPort,
                saveTaskPort,
                loadProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalSetTaskBudgetUseCase(pureService);
    }

    @Bean
    public CloneProjectWbsUseCase cloneProjectWbsUseCase(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadProjectPort loadProjectPort,
            SaveProjectPort saveProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        CloneProjectWbsService service = new CloneProjectWbsService(
                loadTaskPort,
                saveTaskPort,
                loadProjectPort,
                saveProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalCloneProjectWbsUseCase(service);
    }
}
