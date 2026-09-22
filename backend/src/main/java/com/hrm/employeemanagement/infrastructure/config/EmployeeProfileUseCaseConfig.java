package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.employee.EmployeeProfileService;
import com.hrm.employeemanagement.infrastructure.transaction.employee.TransactionalEmployeeProfileServiceDecorator;

@Configuration
public class EmployeeProfileUseCaseConfig {

    @Bean
    public TransactionalEmployeeProfileServiceDecorator employeeProfileService(LoadEmployeePort loadEmployeePort,
                                                         SaveEmployeePort saveEmployeePort,
                                                         LoadUserPort loadUserPort,
                                                         LoadOrgUnitPort loadOrgUnitPort,
                                                         AuthorizationService authorizationService) {
        EmployeeProfileService service = new EmployeeProfileService(loadEmployeePort, saveEmployeePort,
                loadUserPort, loadOrgUnitPort, authorizationService);
        return new TransactionalEmployeeProfileServiceDecorator(service);
    }

    @Bean
    public com.hrm.employeemanagement.application.port.inbound.employee.DeclareOutsourcedEmployeeUseCase declareOutsourcedEmployeeUseCase(
            LoadEmployeePort loadEmployeePort,
            SaveEmployeePort saveEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService,
            com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort saveAuditLogPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository skillCatalogRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository employeeSkillRepository) {
        com.hrm.employeemanagement.application.service.employee.DeclareOutsourcedEmployeeService service =
                new com.hrm.employeemanagement.application.service.employee.DeclareOutsourcedEmployeeService(
                        loadEmployeePort,
                        saveEmployeePort,
                        loadOrgUnitPort,
                        loadUserPort,
                        authorizationService,
                        saveAuditLogPort,
                        skillCatalogRepository,
                        employeeSkillRepository
                );
        return new com.hrm.employeemanagement.infrastructure.transaction.employee.TransactionalDeclareOutsourcedEmployeeServiceDecorator(service);
    }
}
