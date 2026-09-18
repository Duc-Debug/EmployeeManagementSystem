package com.hrm.employeemanagement.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeDataFileParser;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeImportTemplateGenerator;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.importdata.EmployeeImportService;
import com.hrm.employeemanagement.infrastructure.transaction.importdata.TransactionalImportUseCaseDecorator;

@Configuration
public class ImportUseCaseConfig {

    @Bean
    public TransactionalImportUseCaseDecorator employeeImportUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            LoadRolePort loadRolePort,
            LoadEmployeePort loadEmployeePort,
            SaveEmployeePort saveEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            PasswordEncoderPort passwordEncoder,
            SaveAuditLogPort saveAuditLogPort,
            List<EmployeeDataFileParser> fileParsers,
            List<EmployeeImportTemplateGenerator> templateGenerators
    ) {
        EmployeeImportService service = new EmployeeImportService(
                authorizationService,
                loadUserPort,
                saveUserPort,
                loadRolePort,
                loadEmployeePort,
                saveEmployeePort,
                loadOrgUnitPort,
                passwordEncoder,
                saveAuditLogPort,
                fileParsers,
                templateGenerators
        );

        return new TransactionalImportUseCaseDecorator(service, service, service);
    }
}
