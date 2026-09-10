package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.DeleteTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskDependenciesUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.DeleteTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.task.TaskDependencyService;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalTaskDependencyServiceDecorator;

@Configuration
public class TaskDependencyUseCaseConfig {

    @Bean
    public TaskDependencyService taskDependencyService(
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskDependencyPort loadDependencyPort,
            SaveTaskDependencyPort saveDependencyPort,
            DeleteTaskDependencyPort deleteDependencyPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        return new TaskDependencyService(
                loadProjectPort,
                loadTaskPort,
                loadDependencyPort,
                saveDependencyPort,
                deleteDependencyPort,
                loadUserPort,
                loadEmployeePort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService
        );
    }

    @Bean
    public TransactionalTaskDependencyServiceDecorator transactionalTaskDependencyServiceDecorator(
            TaskDependencyService service) {
        return new TransactionalTaskDependencyServiceDecorator(service);
    }

    @Bean
    public CreateTaskDependencyUseCase createTaskDependencyUseCase(
            TransactionalTaskDependencyServiceDecorator decorator) {
        return decorator;
    }

    @Bean
    public DeleteTaskDependencyUseCase deleteTaskDependencyUseCase(
            TransactionalTaskDependencyServiceDecorator decorator) {
        return decorator;
    }

    @Bean
    public GetTaskDependenciesUseCase getTaskDependenciesUseCase(
            TransactionalTaskDependencyServiceDecorator decorator) {
        return decorator;
    }
}
